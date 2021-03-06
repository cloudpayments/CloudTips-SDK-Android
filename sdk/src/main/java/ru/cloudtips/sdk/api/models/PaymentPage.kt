package ru.cloudtips.sdk.api.models

import com.google.gson.annotations.SerializedName

data class PaymentPage(

	@SerializedName("nameText") val nameText: String?,
	@SerializedName("avatarUrl") val avatarUrl: String?,
	@SerializedName("paymentMessage") val paymentMessage: PaymentPageText?,
	@SerializedName("successMessage") val successMessage: PaymentPageText?,
	@SerializedName("amount") val amount: PaymentPageAmount?)

data class PaymentPageText(
	@SerializedName("ru") val ru: String?,
	@SerializedName("en") val en: String?)

data class PaymentPageAmount(
	@SerializedName("constraints") val constraints: ArrayList<AmountConstraint>?)

data class AmountConstraint(
	@SerializedName("type") val type: String?,
	@SerializedName("currency") val currency: String?,
	@SerializedName("value") val value: Int)