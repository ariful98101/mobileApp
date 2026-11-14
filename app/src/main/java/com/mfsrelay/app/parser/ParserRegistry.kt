package com.mfsrelay.app.parser

import com.mfsrelay.app.settings.DefaultMethodSettings
import com.mfsrelay.app.settings.SettingsManager

/** Outcome of trying to classify+parse one incoming SMS. */
data class SmsClassificationResult(
    val parsed: ParsedSms
)

private val PARSERS: List<SmsParser> = listOf(BkashParser, RocketParser, NagadParser)

private val DEFAULTS_BY_METHOD = DefaultMethodSettings.all().associateBy { it.method }

/**
 * Tries every enabled method's parser against an incoming SMS's sender+body, in order
 * (bKash, Rocket, Nagad), and returns the first one whose sender pattern matches.
 */
class ParserRegistry(private val settingsManager: SettingsManager) {

    suspend fun classify(sender: String, body: String): SmsClassificationResult? {
        for (parser in PARSERS) {
            val defaults = DEFAULTS_BY_METHOD[parser.method] ?: continue
            val settings = settingsManager.getMethodSettings(defaults)
            if (!settings.enabled) continue
            if (!parser.matchesSender(sender, settings)) continue

            val parsed = parser.parse(body, settings)
            return SmsClassificationResult(parsed)
        }
        return null
    }
}
