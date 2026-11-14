# MFS SMS Relay (Android)

Merchant-side relay app for the manual MFS (bKash / Rocket / Nagad) payment-verification
system. It runs on the merchant's own Android phone, watches for incoming payment-confirmation
SMS, extracts the transaction details, and POSTs them to a WordPress/WooCommerce plugin's REST
API (built separately) so the plugin can match them against customer-submitted TrxIDs and
release held orders.

This is **not** a Play Store app. It requests `RECEIVE_SMS`/`READ_SMS`, which Google Play
heavily restricts to a short allow-list of core messaging-app use cases that this does not
qualify for. **Distribute it by sideloading the APK (direct install) or through a private/
internal distribution channel**, not the Play Store.

## Required one-time setup before this is production-ready

### 1. Tune the SMS sender patterns and regex per method

Real bKash/Rocket/Nagad SMS wording, shortcodes, and sender IDs vary by operator, SIM, account
type (personal vs agent/merchant), and app language/version. The defaults shipped in
`settings/MethodSettings.kt` are **best-effort placeholders** based on commonly reported SMS
formats, e.g.:

```
"You have received Tk 500.00 from 01812345678. TrxID 9H7A6B5C4D at 14/11/2026 09:45"
```

Before relying on this app in production:

1. Install the app, grant SMS permissions, and let it run for a day on the actual SIM/number
   you plan to use.
2. Send yourself (or collect) real sample payment SMS from bKash, Rocket, and Nagad.
3. Open **Settings** in the app and, for each method, check/update:
   - **Sender patterns** - comma-separated sender IDs/shortcodes/substrings matched
     case-insensitively against the SMS "From" address.
   - **Transaction ID regex**, **Amount regex**, **Customer phone regex** - each must contain
     exactly one capturing group; that group's text becomes the extracted value.
4. Use "Retry" on any row that shows the wrong `trx_id`/`amount`/phone to confirm your updated
   pattern after fixing it (or just wait for the next real SMS).

All of this is editable from Settings; you never need to rebuild the app to adjust patterns.

### 2. Server connection

In **Settings**, set:
- **Server base URL** - e.g. `https://yourshop.example.com` (no trailing slash needed).
- **API key** - generated for this device in the WordPress plugin's "Devices" admin screen.
- **Device ID** is generated automatically on first launch and shown read-only (with a copy
  button) - register that same UUID in the plugin's Devices screen.

Use **Test connection** to call `GET /wp-json/mfs/v1/ping` and confirm the key/URL/device id are
all correct before relying on the app.

### 3. Permissions and background reliability

- Grant SMS permissions via the in-app rationale screen (shown on first launch) or from
  Settings any time.
- Grant notification permission (Android 13+) so the persistent "MFS SMS Relay is listening"
  notification can show.
- Use Settings → "Request battery optimization exemption" so Android is less likely to kill the
  app/stop SMS delivery in Doze or under aggressive OEM battery management. On some phones
  (Xiaomi/Oppo/Vivo/etc.) you may also need to allow "autostart" / disable extra battery
  restrictions from the OEM's own settings app - this varies by device and isn't something the
  app itself can control.

## Architecture / pipeline

```
SMS arrives
  -> SmsReceiver (BroadcastReceiver, android.provider.Telephony.SMS_RECEIVED_ACTION)
     -> ParserRegistry picks the first enabled method whose sender pattern matches
        -> BkashParser / RocketParser / NagadParser extract trx_id / amount / customer_phone
     -> Room: insert SmsLogEntry(status = PENDING_UPLOAD)
     -> WorkScheduler enqueues a OneTimeWorkRequest<SmsUploadWorker> (network-constrained,
        exponential backoff), uniquely keyed per row id
          -> SmsUploadWorker -> SmsLogRepository.attemptUpload()
             -> POST /wp-json/mfs/v1/sms-webhook via Retrofit (MfsApiService)
             -> response matched:true  -> Room row set to MATCHED
             -> response matched:false -> Room row set to SUBMITTED (stored server-side; the
                                          server's 2-min cron keeps retrying the match)
             -> 4xx error               -> Room row set to FAILED (errorCode/message stored);
                                          Worker returns Result.failure() - no auto-retry
             -> network/5xx error       -> Room row stays FAILED but Worker returns
                                          Result.retry() so WorkManager retries with backoff
```

The UI (`MainScreen`) observes the Room table directly via a `Flow`, so the list updates live as
rows are inserted/updated by the receiver/worker. Each row shows a method badge, masked phone,
trx id, amount, timestamp, and a status chip (⏳ Pending / ✅ Submitted / ❌ Failed / 🔗 Matched).
FAILED rows show a **Retry** button; the top bar has **Retry all failed**. Both retry paths reset
the row to `PENDING_UPLOAD` and re-enqueue the *same* row id as `client_submission_id`, so the
server's `(method, trx_id)` idempotency rule (API_CONTRACT.md §2) recognizes it as a resend
rather than a new submission.

Manual dependency injection: `MfsRelayApplication` builds and lazily caches the Room database,
`SettingsManager` (Jetpack DataStore), `ApiClientProvider` (Retrofit/OkHttp, rebuilt only when
the configured base URL changes; auth headers are read live from settings per-request), the
repository, and the parser registry. `SmsReceiver` and `SmsUploadWorker` read these off the
`Application` instance since the OS/WorkManager instantiate them directly.

## API contract

The Retrofit interface (`data/remote/MfsApiService.kt`) and DTOs (`data/remote/dto/*`) implement
`API_CONTRACT.md` exactly: `GET /wp-json/mfs/v1/ping`, `POST /wp-json/mfs/v1/sms-webhook`, the
`X-MFS-API-Key` / `X-MFS-Device-Id` / `Content-Type` headers, and all JSON field names. Do not
rename fields on either side without updating the other.

## CI/CD (GitHub Actions)

`.github/workflows/build-apk.yml`:
- **Push to `main`** (or a PR into it) → builds a **debug** APK, uploaded as a workflow
  artifact (`mfs-sms-relay-debug`).
- **Tag matching `v*`** (e.g. `v1.0.0`) → builds a **signed release** APK and publishes it as a
  GitHub Release with the APK attached.

### Required repository secrets (for the release job)

| Secret              | Meaning                                                        |
|---------------------|-----------------------------------------------------------------|
| `KEYSTORE_BASE64`   | Base64-encoded contents of your release `.keystore`/`.jks` file |
| `KEYSTORE_PASSWORD` | Keystore password                                               |
| `KEY_ALIAS`         | Key alias inside the keystore                                   |
| `KEY_PASSWORD`      | Password for that key alias (often same as keystore password)   |

### Generating a release keystore

```bash
keytool -genkeypair -v \
  -keystore mfs-relay-release.keystore \
  -alias mfs_relay_key \
  -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for a keystore password, your name/org details, and a key password (press
Enter to reuse the keystore password). **Keep this file and its passwords safe** - losing it
means you can never publish an update signed with the same key.

Then base64-encode it for the GitHub secret:

```bash
base64 -w0 mfs-relay-release.keystore > mfs-relay-release.keystore.b64
# paste the contents of mfs-relay-release.keystore.b64 into the KEYSTORE_BASE64 secret
```

(On macOS, use `base64 -i mfs-relay-release.keystore -o mfs-relay-release.keystore.b64` instead
of `-w0`.)

Add all four secrets under **Repository → Settings → Secrets and variables → Actions**.

Without these secrets, `assembleRelease` still works locally/in CI by falling back to the debug
signing key (see `app/build.gradle`) - useful for testing the build, but that APK is **not**
suitable for distribution as a stable release.

## Opening the project

This is a standard Gradle multi-module Android project (`settings.gradle`, root `build.gradle`,
`app/build.gradle`). Open the `android-app/` folder directly in Android Studio (Ladybug/2024.2+
recommended for AGP 8.5 / Kotlin 1.9.24 / Compose). The Gradle wrapper jar binary is not checked
into this repo (binaries aren't practical to generate outside Android Studio/a real Gradle
install); Android Studio will offer to create/sync the wrapper automatically on first open, or
run `gradle wrapper` yourself once if you have a local Gradle install. `gradle/wrapper/gradle-wrapper.properties`
already pins the Gradle version (8.7) that `gradle wrapper` will use.

## Notes on data captured and sent

Per `API_CONTRACT.md`, every matched SMS's **full, unmodified `raw_message`** is sent to the
server (kept there for manual audit/review). Only the method, sender, extracted trx id/amount/
customer phone, timestamp, and a local correlation id are otherwise sent - no other SMS content,
contacts, or device data leaves the phone.
