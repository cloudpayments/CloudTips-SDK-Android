package ru.cloudtips.sdk.api.interfaces

import io.reactivex.Single
import retrofit2.http.*
import ru.cloudtips.sdk.api.models.*

interface TipsApi {

    @GET("layouts/list/{phoneNumber}")
    fun getLayout(@Path("phoneNumber") phoneNumber: String): Single<ArrayList<Layout>>

    @POST("auth/offlineregister")
    fun offlineRegister(@Body body: OfflineRegisterRequestBody): Single<ArrayList<Layout>>

    @GET("paymentpages/{layoutId}")
    fun getPaymentPage(@Path("layoutId") layoutId: String): Single<PaymentPage>

    @POST("payment/publicId")
    fun getPublicId(@Body body: GetPublicIdRequestBody): Single<PublicId>

    @GET("payment/fee")
    fun getPaymentFee(@Query("amount") amount: Double, @Query("layoutId") layoutId: String): Single<PaymentFee>

    @POST("captcha/verify")
    fun verify(@Body body: VerifyRequestBody): Single<VerifyResponse>

    @POST("payment/auth")
    fun auth(@Body body: PaymentRequestBody): Single<PaymentResponse>

    @POST("payment/post3ds")
    fun postThreeDs(@Body body: PostThreeDsRequestBody): Single<PaymentResponse>
}

