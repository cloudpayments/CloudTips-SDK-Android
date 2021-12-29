package ru.cloudtips.sdk.api.models

import com.google.gson.annotations.SerializedName

data class PaymentPage(

    @SerializedName("nameText") val nameText: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("paymentMessage") val paymentMessage: PaymentPageText?,
    @SerializedName("successMessage") val successMessage: PaymentPageText?,
    @SerializedName("amount") val amount: PaymentPageAmount?,
    @SerializedName("payerFee") val payerFee: PayerFee?
)

data class PaymentPageText(
    @SerializedName("ru") val ru: String?,
    @SerializedName("en") val en: String?
)

data class PaymentPageAmount(val range: PaymentPageRange?)

data class PaymentPageRange(
    val minimal: Double?,
    val maximal: Double?,
    val fixed: Double?,
)

data class PayerFee(
    @SerializedName("enabled") val enabled: Boolean?,
    @SerializedName("initialState") private val initialState: String?
) {
    fun getIsEnabled(): Boolean = initialState == "Enabled"
}