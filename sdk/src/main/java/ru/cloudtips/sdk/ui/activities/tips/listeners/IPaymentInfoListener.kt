package ru.cloudtips.sdk.ui.activities.tips.listeners

import ru.cloudtips.sdk.helpers.PayType

interface IPaymentInfoListener : IHeaderCloseListener {
    fun onPayInfoClick()
    fun onPaymentSuccess(payType: PayType)
    fun onPaymentFailure(payType: PayType)
}