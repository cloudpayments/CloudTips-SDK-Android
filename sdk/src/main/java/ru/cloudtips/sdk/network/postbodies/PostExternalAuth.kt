package ru.cloudtips.sdk.network.postbodies

import com.google.gson.annotations.SerializedName

data class PostExternalAuth(
    val layoutId: String,
    val amount: Double,
    val currency: String = "RUB",
    val feeFromPayer: Boolean?,
    val cardholderName: String = "Cloudtips SDK",
    @SerializedName("name") val payerName: String?,
    @SerializedName("comment") val payerComment: String?,
    @SerializedName("payerFeedback") val payerFeedback: String?,
    val rating: PostComponentsRating?,
    val device: PostDeviceInfo? = PostDeviceInfo(),
    val successRedirectUrl: String?,
    val failedRedirectUrl: String?
) {
}


