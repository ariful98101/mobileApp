package com.mfsrelay.app.parser

import com.mfsrelay.app.settings.MethodSettings

/** Result of extracting fields from one MFS SMS body. */
data class ParsedSms(
    val method: String,
    val trxId: String?,
    val amount: Double?,
    val customerPhone: String?
)

/**
 * One parser per MFS method (bKash/Rocket/Nagad). Each implementation applies the
 * user-editable regex patterns from [MethodSettings] (Settings screen -> DataStore) against
 * the raw SMS body - the patterns themselves are best-effort defaults that must be tuned
 * against real sample SMS, see README.
 */
interface SmsParser {
    val method: String

    fun matchesSender(sender: String, settings: MethodSettings): Boolean {
        val candidates = settings.senderPatterns.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (candidates.isEmpty()) return false
        val normalizedSender = sender.trim()
        return candidates.any { pattern ->
            normalizedSender.equals(pattern, ignoreCase = true) ||
                normalizedSender.contains(pattern, ignoreCase = true)
        }
    }

    fun parse(body: String, settings: MethodSettings): ParsedSms
}

/** Shared regex-extraction helper used by all three concrete parsers. */
internal fun extractFirstGroup(regexPattern: String, body: String): String? =
    runCatching {
        Regex(regexPattern, RegexOption.IGNORE_CASE)
            .find(body)
            ?.groups
            ?.get(1)
            ?.value
            ?.trim()
    }.getOrNull()

internal fun extractAmount(regexPattern: String, body: String): Double? =
    extractFirstGroup(regexPattern, body)
        ?.replace(",", "")
        ?.toDoubleOrNull()
