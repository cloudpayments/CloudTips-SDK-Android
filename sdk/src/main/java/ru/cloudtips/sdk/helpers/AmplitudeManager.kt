package ru.cloudtips.sdk.helpers

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import kotlinx.parcelize.Parcelize
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.models.PaymentCardData
import ru.cloudtips.sdk.models.PaymentInfoData
import ru.cloudtips.sdk.network.models.PaymentPageData

class AmplitudeManager(private val context: Context) {
    private val amplitude: Amplitude by lazy {
        Amplitude(Configuration(apiKey = context.getString(R.string.amplitude_apikey), context = context.applicationContext))
    }

    private var layoutId: String? = null
    private var lastEventAction: String? = null

    fun setLayoutId(layoutId: String?) {
        this.layoutId = layoutId
    }

    fun trackLayoutShow(paymentPageData: PaymentPageData?) {
        val fields = paymentPageData?.getAvailableFields() ?: emptyMap()
        val comment = fields[PaymentPageData.AvailableFields.FieldNames.COMMENT]
        val name = fields[PaymentPageData.AvailableFields.FieldNames.NAME]
        val email = fields[PaymentPageData.AvailableFields.FieldNames.EMAIL]
        val city = fields[PaymentPageData.AvailableFields.FieldNames.CITY]
        val phone = fields[PaymentPageData.AvailableFields.FieldNames.PHONE_NUMBER]
        val amountType = when (paymentPageData?.getPaymentType()) {
            PaymentPageData.PaymentType.VOLUNTARY -> "random"
            PaymentPageData.PaymentType.FIXED -> "fix"
            PaymentPageData.PaymentType.MIN -> "random"
            PaymentPageData.PaymentType.GOAL -> "random"
            null -> "random"
        }
        trackEvent(
            "payment_page_open", mutableMapOf(
                "payer_fee" to (paymentPageData?.payerFee?.getIsEnabled() ?: false),
                "payer_fee_initial_state" to (paymentPageData?.payerFee?.getIsOnStart() ?: false),
                "payer_comment" to (comment?.getEnabled() ?: false),
                "payer_comment_required" to (comment?.getRequired() ?: false),
                "payer_name" to (name?.getEnabled() ?: false),
                "payer_name_required" to (name?.getRequired() ?: false),
                "payer_email" to (email?.getEnabled() ?: false),
                "payer_email_required" to (email?.getRequired() ?: false),
                "payer_city" to (city?.getEnabled() ?: false),
                "payer_city_required" to (city?.getRequired() ?: false),
                "payer_phone" to (phone?.getEnabled() ?: false),
                "payer_phone_required" to (phone?.getRequired() ?: false),
                "amount" to amountType,
//                "preset_amount" to (amountType == "preset"),
                "target" to (paymentPageData?.getTarget() != null),
                "rating" to (paymentPageData?.getRating()?.getEnabled() ?: false)

            )
        )
    }

    fun trackPayClick(payType: PayType, payerData: PaymentInfoData?) {
        trackEvent(
            "payment_page_click_pay", mutableMapOf(
                "pay_by" to payType.name.lowercase(),
                "amount_entered" to payerData?.getAmount(),
                "payer_fee_is_filled" to (payerData?.feeFromPayer ?: false),
                "payer_comment_is_filled" to (payerData?.sender?.comment != null),
                "payer_name_is_filled" to (payerData?.sender?.name != null),
                "payer_email_is_filled" to (payerData?.sender?.email != null),
                "payer_city_is_filled" to (payerData?.sender?.city != null),
                "payer_phone_is_filled" to (payerData?.sender?.phone != null),
                "rating_is_filled" to (payerData?.rating?.score ?: 0),
                "rating_components" to payerData?.rating?.components?.mapIndexed { index, s -> s + "_" + index }?.joinToString(",")
            )
        )
    }

    fun trackCardOpen() {
        trackEvent(
            "payment_page_open_card_data", mutableMapOf(
                "pay_by" to PayType.CARD.name.lowercase(),
                "save_card" to false,
                "oneStringCardPayment" to false
            )
        )
    }

    fun trackCardPay(cardData: PaymentCardData?) {
        trackEvent(
            "payment_page_card_data", mutableMapOf(
                "pay_by" to PayType.CARD.name.lowercase(),
                "insert_card_date" to (cardData?.date != null),
                "save_card" to false,
                "deleted_cards" to false,
                "pay_by_save_card" to false,
                "oneStringCardPayment" to false
            )
        )
    }

    fun track3dsOpen(payType: PayType) {
        trackEvent(
            "payment_page_3ds", mutableMapOf(
                "pay_by" to payType.name.lowercase()
            )
        )
    }

    fun trackSuccessOpen(payType: PayType) {
        trackEvent(
            "payment_page_success_page", mutableMapOf(
                "pay_by" to payType.name.lowercase()
            )
        )
    }

    fun trackFailureOpen(payType: PayType, errorMessage: String?) {
        trackEvent(
            "payment_page_error_page", mutableMapOf(
                "pay_by" to payType.name.lowercase(),
                "error_text" to errorMessage
            )
        )
    }

    fun trackPageClosed(payType: PayType?, payerData: PaymentInfoData?) {
        trackEvent(
            "payment_page_closed", mutableMapOf(
                "step" to lastEventAction,
                "pay_by" to payType?.name?.lowercase(),
                "amount_entered" to payerData?.getAmount(),
                "payer_fee_is_filled" to (payerData?.feeFromPayer ?: false),
                "payer_comment_is_filled" to (payerData?.sender?.comment != null),
                "payer_name_is_filled" to (payerData?.sender?.name != null),
                "payer_email_is_filled" to (payerData?.sender?.email != null),
                "payer_city_is_filled" to (payerData?.sender?.city != null),
                "payer_phone_is_filled" to (payerData?.sender?.phone != null),
                "rating_is_filled" to (payerData?.rating?.score ?: 0),
                "rating_components" to payerData?.rating?.components?.mapIndexed { index, s -> s + "_" + index }?.joinToString(",")
            )
        )
    }

    private fun trackEvent(action: String, params: MutableMap<String, Any?>) {
        params.putAll(
            mapOf<String, Any?>(
                "type" to "sdk android",
                "layoutId" to layoutId,
            )
        )
        amplitude.track(
            action, params
        )
        lastEventAction = action

//        Log.e("amplitude", "action: $action, \n params: ${params.entries.joinToString("; ")}")
    }
}

@Parcelize
enum class PayType : Parcelable {
    YANDEXPAY, GOOGLEPAY, TINKOFFPAY, CARD;
}