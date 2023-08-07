package ru.cloudtips.sdk.network

import retrofit2.http.*
import ru.cloudtips.sdk.network.models.*
import ru.cloudtips.sdk.network.postbodies.*

interface ApiRequests {

    @GET("/api/layouts/{layoutId}")
    suspend fun getLink(@Path("layoutId") layoutId: String?): BasicResponse<LinkData>

    @GET("/api/paymentpages/{layoutId}")
    suspend fun getPaymentPage(@Path("layoutId") layoutId: String?): BasicResponse<PaymentPageData>

    @GET("/api/layouts/list/{phoneNumber}")
    suspend fun getLayoutList(@Path("phoneNumber") phoneNumber: String?): BasicResponse<List<Layout>>

    @GET("/api/payment/fee")
    suspend fun getPaymentFee(@Query("layoutId") layoutId: String?, @Query("amount") amount: Double): BasicResponse<PaymentFee>

    @POST("/api/payment/publicid")
    suspend fun getPublicId(@Body body: PostPublicId): BasicResponse<PublicIdData>

    @POST("/api/payment/auth")
    suspend fun postPaymentAuth(@Body body: PostCardAuth, @Header("X-Request-ID") requestId: String): BasicResponse<PaymentAuthData>

    @POST("/api/payment/auth/saved-card")
    suspend fun postPaymentAuthSaved(@Body body: PostCardSavedAuth, @Header("X-Request-ID") requestId: String): BasicResponse<PaymentAuthData>

    @POST("/api/payment/post3ds")
    suspend fun postPayment3ds(@Body body: PostPayment3ds): BasicResponse<PaymentAuthData>

    @POST("captcha/verify")
    suspend fun postAuthVerify(@Body body: PostAuthVerify): BasicResponse<AuthVerifyData>

    @POST("api/payment/sbp")
    suspend fun postPaymentSbp(@Body body: PostExternalAuth, @Header("X-Request-ID") requestId: String): BasicResponse<PaymentExternalData>

    @GET("https://qr.nspk.ru/proxyapp/c2bmembers.json")
    suspend fun getSbpList(): SbpListData

    @GET("/payment/cards")
    suspend fun getSavedCards(@Query("ExternalId") externalId: String?): BasicResponse<List<SavedCardData>>

    @POST("api/payment/tpay")
    suspend fun postPaymentTinkoff(@Body body: PostExternalAuth, @Header("X-Request-ID") requestId: String): BasicResponse<PaymentExternalData>

    @GET("api/payment/tpay/status")
    suspend fun getPaymentTinkoffStatus(@Query("id") id: String?):BasicResponse<PaymentStatusData>

    @GET("api/payment/sbp/status")
    suspend fun getPaymentSbpStatus(@Query("id") id: String?):BasicResponse<PaymentStatusData>

}