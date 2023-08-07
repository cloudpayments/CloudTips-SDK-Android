package ru.cloudtips.sdk.network.postbodies

import com.google.gson.annotations.SerializedName

data class PostCardAuth(
    val layoutId: String,
    @SerializedName("cardCryptogramPacket") val cryptogram: String,
    val amount: Double,
    val currency: String = "RUB",
    val feeFromPayer: Boolean,
    val cardholderName: String = "Cloudtips SDK",
    @SerializedName("name") val payerName: String?,
    @SerializedName("comment") val payerComment: String?,
    @SerializedName("payerFeedback") val payerFeedback: String?,
    val rating: PostComponentsRating?,
    val captchaVerificationToken: String?,
    val saveCard: Boolean,
    val externalId: String?
)


