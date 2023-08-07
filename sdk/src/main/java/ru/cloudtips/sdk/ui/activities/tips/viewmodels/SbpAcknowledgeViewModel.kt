package ru.cloudtips.sdk.ui.activities.tips.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import ru.cloudtips.sdk.helpers.AcknowledgeStatus
import ru.cloudtips.sdk.helpers.SbpHelper
import ru.cloudtips.sdk.network.models.PaymentExternalData
import ru.cloudtips.sdk.network.models.PaymentStatusData
import ru.cloudtips.sdk.networkClient
import ru.cloudtips.sdk.sharedPrefs

class SbpAcknowledgeViewModel : ViewModel() {

    private val TRY_COUNT = 2
    private val DELAY_TIME = 10000L
    fun startAcknowledge(data: PaymentExternalData?, item: SbpHelper.SbpBank): LiveData<AcknowledgeStatus> {
        return liveData(Dispatchers.IO) {
            for (i in 0 until TRY_COUNT) {
                val response = networkClient.getPaymentSbpStatus(data?.identifier)
                if (response.succeed) {
                    val status = response.data?.getStatus()
                    if (status == PaymentStatusData.Status.Success) {
                        saveSbpBank(item)
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

    private fun saveSbpBank(item: SbpHelper.SbpBank) {
        sharedPrefs.sbpSavedBankSchema = item.schema
    }
}