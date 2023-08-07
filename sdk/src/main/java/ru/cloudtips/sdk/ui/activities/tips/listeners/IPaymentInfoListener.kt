package ru.cloudtips.sdk.ui.activities.tips.listeners

import ru.cloudtips.sdk.helpers.PayType
import ru.cloudtips.sdk.network.models.PaymentExternalData

interface IPaymentInfoListener : IHeaderCloseListener {
    fun onPaymentSuccess(payType: PayType)
    fun onPaymentFailure(payType: PayType)
    fun onPayInfoTinkoffLaunch(data: PaymentExternalData?)
    fun onPayInfoSbpLaunch()
}