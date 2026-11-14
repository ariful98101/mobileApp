package com.mfsrelay.app.parser

import com.mfsrelay.app.settings.MethodSettings

/**
 * Parses Rocket (DBBL Mobile Banking) "received money" SMS. Rocket's wording and field
 * labels (e.g. "TxnId" vs "Ref") have varied across SMS campaigns/years - verify against real
 * sample messages and tune in Settings before relying on this in production.
 */
object RocketParser : SmsParser {
    override val method: String = "rocket"

    override fun parse(body: String, settings: MethodSettings): ParsedSms = ParsedSms(
        method = method,
        trxId = extractFirstGroup(settings.trxIdRegex, body),
        amount = extractAmount(settings.amountRegex, body),
        customerPhone = extractFirstGroup(settings.customerPhoneRegex, body)
    )
}
