package ru.cloudtips.sdk.network.postbodies

import com.google.gson.annotations.SerializedName

data class PostCardSavedAuth(
    val layoutId: String,
    @SerializedName("CvvCryptogramPacket") val cryptogram: String?,
    val amount: Double,
    val currency: String = "RUB",
    val feeFromPayer: Boolean,
    val cardholderName: String = "Cloudtips SDK",
    @SerializedName("name") val payerName: String?,
    @SerializedName("comment") val payerComment: String?,
    @SerializedName("payerFeedback") val payerFeedback: String?,
    val rating: PostComponentsRating?,
    val captchaVerificationToken: String?,
    val cardId:String?,
    val externalId: String?
)