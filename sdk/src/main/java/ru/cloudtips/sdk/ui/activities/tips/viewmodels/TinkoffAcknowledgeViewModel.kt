package ru.cloudtips.sdk.ui.activities.tips.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import ru.cloudtips.sdk.helpers.AcknowledgeStatus
import ru.cloudtips.sdk.network.models.PaymentExternalData
import ru.cloudtips.sdk.network.models.PaymentStatusData
import ru.cloudtips.sdk.networkClient

class TinkoffAcknowledgeViewModel : ViewModel() {

    private val TRY_COUNT = 2
    private val DELAY_TIME = 10000L
    fun startAcknowledge(data: PaymentExternalData): LiveData<AcknowledgeStatus> {
        return liveData(Dispatchers.IO) {
            for (i in 0 until TRY_COUNT) {
                val response = networkClient.getPaymentTinkoffStatus(data.identifier)
                if (response.succeed) {
                    val status = response.data?.getStatus()
                    if (status == PaymentStatusData.Status.Success) {
                        emit(AcknowledgeStatus.SUCCESS)

                        return@liveData
                    }
                    if (status == PaymentStatusData.Status.Failed) {
                        emit(AcknowledgeStatus.FAIL)
                        return@liveData
                    }
                }
                delay(DELAY_TIME)
            }
            emit(AcknowledgeStatus.TIMEOUT)
        }
    }
}