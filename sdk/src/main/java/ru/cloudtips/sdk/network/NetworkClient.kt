package ru.cloudtips.sdk.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import ru.cloudtips.sdk.BuildConfig
import ru.cloudtips.sdk.models.PaymentInfoData
import ru.cloudtips.sdk.network.models.*
import ru.cloudtips.sdk.network.postbodies.*

import ru.cloudtips.sdk.sharedPrefs
import java.io.IOException
import java.net.CookiePolicy
import java.util.UUID
import java.util.concurrent.TimeUnit

class NetworkClient {
    private val apiUrl = BuildConfig.URL_API
    private val apiRequests: ApiRequests

    private val dispatcher = Dispatchers.IO

    private val mUserAgentInterceptor: Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "CloudTips/SDK-Android")
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private var xRequestId: String? = null
    fun generateXRequestId() {
        xRequestId = UUID.randomUUID().toString()
    }

    fun clearXRequestId() {
        xRequestId = null
    }

    private fun getXRequestId(): String {
        val result = xRequestId ?: UUID.randomUUID().toString()
        xRequestId = result
        return result
    }

    init {
        val cookieManager = java.net.CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = 1

        val defaultHttpClientBuilder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            defaultHttpClientBuilder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        defaultHttpClientBuilder
            .addInterceptor(mUserAgentInterceptor)
            .dispatcher(dispatcher)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .cookieJar(JavaNetCookieJar(cookieManager))

        val retrofitApi = Retrofit.Builder()
            .baseUrl(apiUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(defaultHttpClientBuilder.build())
            .build()

        apiRequests = retrofitApi.create(ApiRequests::class.java)
    }

    //API

    suspend fun getLayoutList(phone: String?): BasicResponse<List<Layout>> {
        return safeApiCall(dispatcher) { apiRequests.getLayoutList(phone) }
    }

    suspend fun getLink(layoutId: String?): BasicResponse<LinkData> {
        return safeApiCall(dispatcher) { apiRequests.getLink(layoutId) }
    }

    suspend fun getPaymentPage(layoutId: String?): BasicResponse<PaymentPageData> {
        return safeApiCall(dispatcher) { apiRequests.getPaymentPage(layoutId) }
    }

    suspend fun getPaymentFee(layoutId: String?, amount: Double): BasicResponse<PaymentFee> {
        return safeApiCall(dispatcher) { apiRequests.getPaymentFee(layoutId, amount) }
    }

    suspend fun getPublicId(layoutId: String?): BasicResponse<PublicIdData> {
        return safeApiCall(dispatcher) { apiRequests.getPublicId(PostPublicId(layoutId)) }
    }

    suspend fun postPaymentAuth(
        layoutId: String,
        info: PaymentInfoData,
        cryptogram: String,
        saveCard: Boolean,
        captcha: String? = null
    ): BasicResponse<PaymentAuthData> {
        val body = PostCardAuth(
            amount = info.getAmount(),
            feeFromPayer = info.getFeeFromPayer(),
            payerName = info.sender?.name,
            payerComment = info.sender?.comment,
            payerFeedback = info.sender?.feedback,
            layoutId = layoutId,
            cryptogram = cryptogram,
            rating = if ((info.rating?.score ?: 0) > 0) PostComponentsRating(
                info.rating?.score,
                info.rating?.components
            ) else null,
            captchaVerificationToken = captcha,
            saveCard = saveCard,
            externalId = if (saveCard) sharedPrefs.cardExternalId else null
        )
        return safeApiCall(dispatcher) { apiRequests.postPaymentAuth(body, getXRequestId()) }
    }

    suspend fun postPaymentSbp(
        layoutId: String,
        info: PaymentInfoData,
        deeplink: String?
    ): BasicResponse<PaymentExternalData> {
        val body = PostExternalAuth(
            amount = info.getAmount(),
            feeFromPayer = info.getFeeFromPayer(),
            payerName = info.sender?.name,
            payerComment = info.sender?.comment,
            payerFeedback = info.sender?.feedback,
            layoutId = layoutId,
            rating = if ((info.rating?.score ?: 0) > 0) PostComponentsRating(
                info.rating?.score,
                info.rating?.components
            ) else null,
            successRedirectUrl = deeplink,
            failedRedirectUrl = deeplink
        )
        return safeApiCall(dispatcher) { apiRequests.postPaymentSbp(body, getXRequestId()) }
    }

    suspend fun postSavedPaymentAuth(
        layoutId: String,
        info: PaymentInfoData,
        cryptogram: String?,
        cardId: String?,
        captcha: String? = null
    ): BasicResponse<PaymentAuthData> {
        val body = PostCardSavedAuth(
            amount = info.getAmount(),
            feeFromPayer = info.getFeeFromPayer(),
            payerName = info.sender?.name,
            payerComment = info.sender?.comment,
            payerFeedback = info.sender?.feedback,
            layoutId = layoutId,
            cryptogram = cryptogram,
            rating = if ((info.rating?.score ?: 0) > 0) PostComponentsRating(
                info.rating?.score,
                info.rating?.components
            ) else null,
            captchaVerificationToken = captcha,
            cardId = cardId,
            externalId = sharedPrefs.cardExternalId
        )
        return safeApiCall(dispatcher) { apiRequests.postPaymentAuthSaved(body, getXRequestId()) }
    }

    suspend fun postPaymentTinkoff(
        layoutId: String,
        info: PaymentInfoData,
        deeplink: String?
    ): BasicResponse<PaymentExternalData> {
        val body = PostExternalAuth(
            amount = info.getAmount(),
            feeFromPayer = info.getFeeFromPayer(),
            payerName = info.sender?.name,
            payerComment = info.sender?.comment,
            payerFeedback = info.sender?.feedback,
            layoutId = layoutId,
            rating = if ((info.rating?.score ?: 0) > 0) PostComponentsRating(
                info.rating?.score,
                info.rating?.components
            ) else null,
            successRedirectUrl = deeplink,
            failedRedirectUrl = deeplink
        )
        return safeApiCall(dispatcher) { apiRequests.postPaymentTinkoff(body, getXRequestId()) }
    }

    suspend fun postPayment3ds(md: String, paRes: String): BasicResponse<PaymentAuthData> {
        return safeApiCall(dispatcher) { apiRequests.postPayment3ds(PostPayment3ds(md, paRes)) }
    }

    suspend fun postAuthVerify(amount: Double, layoutId: String, version: Int, token: String?): BasicResponse<AuthVerifyData> {
        return safeApiCall(dispatcher) { apiRequests.postAuthVerify(PostAuthVerify(amount, layoutId, version, token)) }
    }

    suspend fun getSbpBankList(): ResultWrapper<SbpListData> {
        return safeApiResultWrapper(dispatcher) { apiRequests.getSbpList() }
    }

    suspend fun getSavedCards(): BasicResponse<List<SavedCardData>> {
        return safeApiCall(dispatcher) { apiRequests.getSavedCards(sharedPrefs.cardExternalId) }
    }

    suspend fun getPaymentTinkoffStatus(id: String?): BasicResponse<PaymentStatusData> {
        return safeApiCall(dispatcher) { apiRequests.getPaymentTinkoffStatus(id) }
    }

    suspend fun getPaymentSbpStatus(id: String?): BasicResponse<PaymentStatusData> {
        return safeApiCall(dispatcher) { apiRequests.getPaymentSbpStatus(id) }
    }

    private suspend fun <T> safeApiCall(dispatcher: CoroutineDispatcher, apiCall: suspend () -> BasicResponse<T>): BasicResponse<T> {
        return withContext(dispatcher) {
            try {
                apiCall.invoke()
            } catch (throwable: Throwable) {
                when (throwable) {
                    is IOException -> BasicResponse.getUnknownError()
                    is HttpException -> {
                        val code = throwable.code()
                        convertErrorBody<T>(throwable).apply {
                            this.code = code
                        }
                    }
                    else -> BasicResponse.getUnknownError()
                }
            }
        }
    }

    private fun <T> convertErrorBody(throwable: HttpException): BasicResponse<T> {
        return try {
            throwable.response()?.errorBody()?.string()?.let {
                val moshiAdapter = Moshi.Builder().build().adapter(BasicError::class.java)
                moshiAdapter.fromJson(it)?.toBasicResponse()
            } ?: BasicResponse.getUnknownError()
        } catch (exception: Exception) {
            BasicResponse.getUnknownError()
        }
    }

}

@JsonClass(generateAdapter = true)
data class ValidationErrors(
    val PhoneNumber: List<String>?
) {
    fun getErrors(): List<String> {
        //concat all texts
        return PhoneNumber ?: emptyList()
    }
}

@JsonClass(generateAdapter = true)
class BasicError {
    var succeed: Boolean = false
    var errors: List<String>? = emptyList()
    var validationErrors: ValidationErrors? = null
    var code: Int = 0
    private fun getErrorsList(): List<String> {
        val verr = validationErrors?.getErrors() ?: emptyList()
        val err = errors ?: emptyList()
        return verr.plus(err)
    }

    fun <T> toBasicResponse(): BasicResponse<T> {
        return BasicResponse<T>().apply {
            succeed = this@BasicError.succeed
            errors = this@BasicError.getErrorsList()
            code = this@BasicError.code
        }
    }
}

fun List<String>?.hasNeedCaptcha() = (this != null && this.find { it.equals("Неверно введена капча", true) } != null)

class BasicResponse<T> {
    var succeed: Boolean = true
    internal var errors: List<String>? = emptyList()
    var code: Int = 0
    var data: T? = null

    fun getErrors(): List<String> {
        return errors ?: emptyList()
    }

    companion object {
        fun <T> getUnknownError() = BasicResponse<T>().apply {
            succeed = false
        }
    }
}

private suspend fun <T> safeApiResultWrapper(dispatcher: CoroutineDispatcher, apiCall: suspend () -> T): ResultWrapper<T> {
    return withContext(dispatcher) {
        try {
            ResultWrapper.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> ResultWrapper.NetworkError
                is HttpException -> {
                    ResultWrapper.GenericError(throwable.code(), throwable.response()?.errorBody()?.string())
                }
                else -> {
                    ResultWrapper.GenericError(null, null)
                }
            }
        }
    }
}

sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    data class GenericError(val code: Int? = null, val error: String?) : ResultWrapper<Nothing>()
    object NetworkError : ResultWrapper<Nothing>()
}

