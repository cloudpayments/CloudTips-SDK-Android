package ru.cloudtips.sdk.ui.activities.tips.listeners

interface IPaymentSbpListener {
    fun onSbpPaymentSuccess()
    fun onSbpPaymentFailure()
}