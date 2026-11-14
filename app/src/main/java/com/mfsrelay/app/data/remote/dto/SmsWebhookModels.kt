package com.mfsrelay.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** POST /wp-json/mfs/v1/sms-webhook — request body. Field names/order match API_CONTRACT.md §2 exactly. */
data class SmsWebhookRequest(
    @SerializedName("method") val method: String,
    @SerializedName("sender") val sender: String,
    @SerializedName("customer_phone") val customerPhone: String?,
    @SerializedName("trx_id") val trxId: String?,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("raw_message") val rawMessage: String,
    @SerializedName("received_at") val receivedAt: String,
    @SerializedName("client_submission_id") val clientSubmissionId: String
)

/** POST /wp-json/mfs/v1/sms-webhook — success body (HTTP 200, success:true). */
data class SmsWebhookResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("client_submission_id") val clientSubmissionId: String,
    @SerializedName("matched") val matched: Boolean,
    @SerializedName("payment_id") val paymentId: Long?,
    @SerializedName("order_id") val orderId: Long?,
    @SerializedName("message") val message: String?
)
