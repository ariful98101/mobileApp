package com.mfsrelay.app.parser

import com.mfsrelay.app.settings.MethodSettings

/**
 * Parses bKash "received money" SMS, e.g.:
 * "You have received Tk 500.00 from 01812345678. TrxID 9H7A6B5C4D at 14/11/2026 09:45"
 *
 * The exact wording bKash uses varies (personal vs agent/merchant number, app language,
 * promotional text appended, etc.) - verify these patterns against real messages before
 * relying on this in production; override them from the Settings screen if needed.
 */
object BkashParser : SmsParser {
    override val method: String = "bkash"

    override fun parse(body: String, settings: MethodSettings): ParsedSms = ParsedSms(
        method = method,
        trxId = extractFirstGroup(settings.trxIdRegex, body),
        amount = extractAmount(settings.amountRegex, body),
        customerPhone = extractFirstGroup(settings.customerPhoneRegex, body)
    )
}
