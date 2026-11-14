package com.mfsrelay.app.parser

import com.mfsrelay.app.settings.MethodSettings

/**
 * Parses Nagad "received money" SMS. As with the other two methods, verify the default
 * regex patterns against real sample SMS and tune in Settings before relying on this in
 * production - Nagad's exact wording varies by personal/merchant account type.
 */
object NagadParser : SmsParser {
    override val method: String = "nagad"

    override fun parse(body: String, settings: MethodSettings): ParsedSms = ParsedSms(
        method = method,
        trxId = extractFirstGroup(settings.trxIdRegex, body),
        amount = extractAmount(settings.amountRegex, body),
        customerPhone = extractFirstGroup(settings.customerPhoneRegex, body)
    )
}
