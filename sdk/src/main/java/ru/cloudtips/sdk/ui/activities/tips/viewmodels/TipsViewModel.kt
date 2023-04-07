package ru.cloudtips.sdk.ui.activities.tips.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.wallet.PaymentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.card.Card
import ru.cloudtips.sdk.helpers.PayType
import ru.cloudtips.sdk.models.*
import ru.cloudtips.sdk.network.BasicResponse
import ru.cloudtips.sdk.network.models.*
import ru.cloudtips.sdk.network.models.PaymentAuthStatusCode.*
import ru.cloudtips.sdk.networkClient
import kotlin.math.ceil

class TipsViewModel : ViewModel() {
    private val paymentLayoutId = MutableStateFlow<String?>(null)
    private val paymentPublicId = MutableStateFlow<PublicIdData?>(null)

    private val sumData = MutableStateFlow<Double>(0.0)
    fun getSum() = sumData.asLiveData()

    private val paymentInfoData = MutableLiveData(
        emptyInfoData()
    )

    private fun emptyInfoData() = PaymentInfoData(
        0.0,
        0.0,
        false,
        PaymentInfoSenderData(null, null, null, null, null),
        PaymentInfoRatingData(0, mutableListOf())
    )


    private val paymentPageData = MutableStateFlow<PaymentPageData?>(null)
    private val paymentPageLiveData = paymentPageData.asLiveData()
    fun getPaymentPageData() = paymentPageLiveData
    fun requestPaymentPageData(layoutId: String?, sum: Double?): LiveData<BasicResponse<PaymentPageData>> {
        return liveData(Dispatchers.IO) {
            sumData.emit(sum ?: 0.0)
            paymentLayoutId.emit(layoutId)
            val response = networkClient.getPaymentPage(layoutId)
            val data = response.data
            if (response.succeed && data != null) {
                paymentPageData.emit(data)
                setPresetSettings(data)
            }
            val publicIdResponse = networkClient.getPublicId(layoutId)
            if (publicIdResponse.succeed) {
                paymentPublicId.emit(publicIdResponse.data)
            }
            paymentInfoData.postValue(emptyInfoData())
            paymentCardData.value = null
            emit(response)
        }
    }

    fun getFeeValue(amount: Double): LiveData<Double> {
        return liveData(Dispatchers.IO) {
            val layoutId = paymentLayoutId.value
            val response = networkClient.getPaymentFee(layoutId, amount)
            if (response.succeed) emit(response.data?.amountFromPayer ?: 0.0)
        }
    }


    fun getPaymentInfoData() = paymentInfoData

    fun putPaymentInfoAmountData(
        amount: Double,
        feeAmount: Double,
        feeFromPayer: Boolean
    ) {
        viewModelScope.launch {
            val oldValue = paymentInfoData.value
            oldValue?.apply {
                setAmount(amount)
                setFeeAmount(feeAmount)
                this.feeFromPayer = feeFromPayer
            }
        }
    }


    fun putPaymentInfoSenderData(
        name: String?,
        email: String?,
        phone: String?,
        city: String?,
        comment: String?,
    ) {
        viewModelScope.launch {
            val oldValue = paymentInfoData.value
            oldValue?.sender?.apply {
                this.name = if (!name.isNullOrEmpty()) name else null
                this.email = if (!email.isNullOrEmpty()) email else null
                this.phone = if (!phone.isNullOrEmpty()) phone else null
                this.city = if (!city.isNullOrEmpty()) city else null
                this.comment = if (!comment.isNullOrEmpty()) comment else null
            }
        }
    }

    fun putPaymentInfoRatingData(
        rating: Int,
        ratingComponents: List<RatingComponent>
    ) {
        viewModelScope.launch {
            val newValue = paymentInfoData.value
            newValue?.rating?.apply {
                score = rating
                components.clear()
                components.addAll(ratingComponents.filter { it.selected }.map { it.id ?: "" })
            }
        }
    }

    private val paymentCardData = MutableStateFlow<PaymentCardData?>(null)
    fun putPaymentCardData(number: String?, date: String?, cvc: String?) {
        viewModelScope.launch {
            val data = PaymentCardData(number, date, cvc)
            paymentCardData.emit(data)
        }
    }

    private suspend fun refreshPaymentPageData() {
        val layoutId = paymentLayoutId.value
        val response = networkClient.getPaymentPage(layoutId)
        if (response.succeed && response.data != null) {
            paymentPageData.emit(response.data)
        }
    }

    private suspend fun refreshPaymentAfterSuccess(response: BasicResponse<PaymentAuthData>) {
        if (!response.succeed) return
        val data = response.data
        when (data?.getStatusCode()) {
            SUCCESS -> {
                refreshPaymentPageData()
            }
            NEED3DS,
            UNKNOWN,
            null -> {
            }
        }
    }

    fun launchPayment(captcha: String? = null): LiveData<BasicResponse<PaymentAuthData>> {
        networkClient.generateXRequestId()
        return liveData(Dispatchers.IO) {
            val layoutId = paymentLayoutId.value ?: return@liveData
            val info = paymentInfoData.value ?: return@liveData
            val card = paymentCardData.value ?: return@liveData
            val publicId = paymentPublicId.value?.publicId ?: return@liveData

            val cryptogram = Card.cardCryptogram(card.number ?: "", card.date ?: "", card.cvc ?: "", publicId) ?: ""

            val verifyResponse = postAuthVerify(info.getAmountWithFee(), layoutId, captcha)
            if (verifyResponse.succeed) {
                val verifyCaptcha = verifyResponse.data?.getToken()
                val response = networkClient.postPaymentAuth(layoutId, info, cryptogram, verifyCaptcha)
                refreshPaymentAfterSuccess(response)
                emit(response)
            } else {
                emit(BasicResponse<PaymentAuthData>().apply {
                    succeed = false
                    errors = verifyResponse.getErrors()
                })
            }

        }
    }

    private suspend fun postAuthVerify(amount: Double, layoutId: String, captcha: String? = null): BasicResponse<AuthVerifyData> {
        val version: Int
        val token: String?
        if (captcha.isNullOrEmpty()) {
            version = 3
            token = null
        } else {
            version = 4
            token = captcha
        }
        return networkClient.postAuthVerify(amount, layoutId, version, token)
    }

    fun postPayment3ds(md: String, paRes: String): LiveData<BasicResponse<PaymentAuthData>> {
        return liveData(Dispatchers.IO) {
            val response = networkClient.postPayment3ds(md, paRes)
            refreshPaymentAfterSuccess(response)
            emit(response)
        }
    }

    fun getMerchantId() = paymentPublicId.asLiveData()

    fun launchGPayment(paymentData: PaymentData?): LiveData<BasicResponse<PaymentAuthData>> {
        networkClient.generateXRequestId()
        return liveData(Dispatchers.IO) {
            val paymentInformation = paymentData?.toJson()
            val cryptogram: String
            try {
                val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
                cryptogram = paymentMethodData.getJSONObject("tokenizationData").getString("token")
                val layoutId = paymentLayoutId.value ?: return@liveData
                val info = paymentInfoData.value ?: return@liveData

                val response = networkClient.postPaymentAuth(layoutId, info, cryptogram, null)
                refreshPaymentAfterSuccess(response)
                emit(response)
            } catch (e: Exception) {
                Log.e("launchGPayment", e.message, e)
            }
        }
    }

    fun launchYPayment(cryptogram: String): LiveData<BasicResponse<PaymentAuthData>> {
        networkClient.generateXRequestId()
        return liveData(Dispatchers.IO) {
            val layoutId = paymentLayoutId.value ?: return@liveData
            val info = paymentInfoData.value ?: return@liveData

            val response = networkClient.postPaymentAuth(layoutId, info, cryptogram, null)
            refreshPaymentAfterSuccess(response)
            emit(response)
        }
    }

    fun trackPayClick(payType: PayType) {
        viewModelScope.launch {
            amplitude.trackPayClick(payType, paymentInfoData.value)
        }
    }

    fun trackCardPayClick() {
        viewModelScope.launch {
            amplitude.trackCardPay(paymentCardData.value)
        }
    }

    fun trackPageClosed(payType: PayType? = null) {
        viewModelScope.launch {
            amplitude.trackPageClosed(payType, paymentInfoData.value)
        }
    }

    private val presetSettingsData = MutableStateFlow<PresetInfoData?>(null)
    fun getPresetSettings() = presetSettingsData.asLiveData()
    private suspend fun setPresetSettings(data: PaymentPageData?) {
        if (data == null) {
            presetSettingsData.emit(null)
            return
        }
        val range = Pair(data.getAmount()?.range?.getMinimal() ?: 0.0, data.getAmount()?.range?.getMaximal() ?: 0.0)
        val sum = sumData.value
        val percents =
            data.percents?.getPercents()?.map { it.toDouble() }?.filter { ceil(sum * it / 100.0) in range.first..range.second } ?: emptyList()
        val result: PresetInfoData? = if (sum > 0 && percents.isNotEmpty()) {
            PresetInfoData.buildByPercent(percents, sum, range)
        } else {
            val presets = data.getPresets()
            if (presets != null && presets.getEnabled()) {
                if (presets.getAction() == "Add") {
                    PresetInfoData.buildByAdd(presets.getValues())
                } else {
                    PresetInfoData.buildByValue(presets.getValues(), range)
                }
            } else {
                null
            }
        }

        presetSettingsData.emit(result)
    }
}