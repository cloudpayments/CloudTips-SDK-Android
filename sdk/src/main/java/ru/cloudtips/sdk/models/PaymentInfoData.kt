package ru.cloudtips.sdk.models

data class PaymentInfoData(
    private var amount: Double,
    private var feeAmount: Double,
    private var feeFromPayer: Boolean,
    val sender: PaymentInfoSenderData? = null,
    val rating: PaymentInfoRatingData? = null
) {
    fun getAmount(): Double = amount
    fun setAmount(value: Double) {
        amount = value
    }

    fun setFeeAmount(value: Double) {
        feeAmount = value
    }

    fun getAmountWithFee(): Double = if (feeFromPayer) feeAmount else amount

    fun getFeeFromPayer() = feeFromPayer
    fun setFeeFromPayer(value: Boolean) {
        feeFromPayer = value
    }

    companion object {
        fun empty(): PaymentInfoData {
            return PaymentInfoData(
                0.0,
                0.0,
                false,
                PaymentInfoSenderData(null, null, null),
                PaymentInfoRatingData(0, mutableListOf())
            )
        }
    }
}

data class PaymentInfoSenderData(var name: String?, var comment: String?, var feedback:String?)

data class PaymentInfoRatingData(var score: Int, val components: MutableList<String>)

data class PaymentCardData(val number: String?, val date: String?, val cvc: String?, val save: Boolean)

data class PaymentSavedCardData(val cardId: String?, val cvc: String?)