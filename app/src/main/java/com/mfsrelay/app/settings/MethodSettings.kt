package com.mfsrelay.app.settings

/**
 * Per-MFS-method configuration, editable from the Settings screen.
 *
 * [senderPatterns] is a comma-separated list of sender IDs/shortcodes or substrings to match
 * against the SMS "from" address (case-insensitive substring match - operators route these
 * through different shortcodes/alphanumeric IDs depending on region/SIM, so this is meant to
 * be tuned by the user against real SMS they actually receive).
 *
 * [trxIdRegex], [amountRegex], [customerPhoneRegex] are applied against the SMS body. Each
 * must contain exactly one capturing group - group 1 is the extracted value. These are
 * best-effort defaults based on commonly reported bKash/Rocket/Nagad wording and WILL need
 * verification/tuning against real sample messages before relying on this in production
 * (see README "Required setup: tune the SMS patterns").
 */
data class MethodSettings(
    val method: String,
    val enabled: Boolean,
    val senderPatterns: String,
    val trxIdRegex: String,
    val amountRegex: String,
    val customerPhoneRegex: String
)

object DefaultMethodSettings {

    val BKASH = MethodSettings(
        method = "bkash",
        enabled = true,
        senderPatterns = "bKash,16247",
        // e.g. "You have received Tk 500.00 from 01812345678. TrxID 9H7A6B5C4D at 14/11/2026 09:45"
        trxIdRegex = "TrxID\\s+([A-Za-z0-9]+)",
        amountRegex = "Tk\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        customerPhoneRegex = "from\\s+(01[0-9]{9})"
    )

    val ROCKET = MethodSettings(
        method = "rocket",
        enabled = true,
        senderPatterns = "Rocket,16216",
        // e.g. "You have received Tk500.00 from 01812345678. Ref 9H7A6B5C4D TxnId 123ABC456"
        trxIdRegex = "TxnId\\s*[:\\-]?\\s*([A-Za-z0-9]+)",
        amountRegex = "Tk\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        customerPhoneRegex = "from\\s+(01[0-9]{9})"
    )

    val NAGAD = MethodSettings(
        method = "nagad",
        enabled = true,
        senderPatterns = "Nagad,16167",
        // e.g. "You have received Tk 500.00 from 01812345678. TrxID: 9H7A6B5C4D"
        trxIdRegex = "TrxID\\s*[:\\-]?\\s*([A-Za-z0-9]+)",
        amountRegex = "Tk\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        customerPhoneRegex = "from\\s+(01[0-9]{9})"
    )

    fun all(): List<MethodSettings> = listOf(BKASH, ROCKET, NAGAD)
}
