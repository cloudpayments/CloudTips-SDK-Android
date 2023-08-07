package ru.cloudtips.sdk.network.models

import android.annotation.SuppressLint
import android.os.Parcelable
import android.text.format.DateFormat
import kotlinx.parcelize.Parcelize
import ru.cloudtips.sdk.BuildConfig
import ru.cloudtips.sdk.helpers.CommonHelper
import java.util.*
import kotlin.math.ceil

@Parcelize
data class PaymentPageData(
    val layoutId: String?,
    private val amount: Amount?,
    private val availableFields: AvailableFields?,
    private val backgroundUrl: String?,
    private val logoUrl: String?,
    private val backgroundColor: String?,
    private val linksColor: String?,
    private val buttonsColor: String?,
    private val failMessage: Message?,
    private val avatarUrl: String?,
    private val nameText: String?,
    val payerFee: PayerFee?,
    private val paymentMessage: Message?,
    private val successMessage: Message?,
    private val target: Target?,
    val title: String?,
    val url: String?,
    private val rating: RatingData?,
    private val googlePayEnabled: Boolean?,
    private val sbpPayEnabled: Boolean?,
    private val tinkoffPayEnabled: Boolean?,
    val percents: Percents?
) : Parcelable {

    fun getBackground(): String? = backgroundUrl

    fun getPaymentMessage() = paymentMessage?.getText()

    fun getUserAvatar(): String? {
        //hardcode for default avatar from server
        return if (BuildConfig.DEBUG && avatarUrl != null && avatarUrl.contains("avatar-default")) null
        else avatarUrl
    }

    fun getLogo(): String? {
        //hardcode for default avatar from server
        return if (BuildConfig.DEBUG && logoUrl != null && logoUrl.contains("cloudtips-logo")) null
        else logoUrl
    }
    fun getUserName() = nameText

    fun getRating() = rating

    fun getSuccessMessage() = successMessage?.getText()

    fun getGooglePayEnabled() = googlePayEnabled ?: false

    fun getSbpEnabled() = sbpPayEnabled ?: false

    fun getTinkoffPayEnabled() = tinkoffPayEnabled ?: false

    fun getBackgroundColor(): Int? = CommonHelper.getColorByString(backgroundColor)

    fun getLinksColor(): Int? = CommonHelper.getColorByString(linksColor)

    fun getButtonsColor(): Int? = CommonHelper.getColorByString(buttonsColor)

    fun getPresets() = amount?.amountPresetSettings

    fun getPresetSum(sum: Double): Double {
        val default = percents?.getDefault() ?: return 0.0
        val minimal = amount?.range?.getMinimal() ?: 0.0
        val maximal = amount?.range?.getMaximal() ?: 0.0

        val presetSum = ceil(sum * default / 100.0)
        return if (presetSum in minimal..maximal) presetSum else 0.0
    }

    fun getTarget(): Target? = target

    @Parcelize
    data class AfterPaymentActions(
        val emailSending: EmailSending?
    ) : Parcelable {
        @Parcelize
        data class EmailSending(
            val enabled: Boolean?,
            val text: String?
        ) : Parcelable
    }

    @Parcelize
    data class Amount(
        val range: AmountRange?,
        val currency: String? = "RUB",
        internal val amountPresetSettings: AmountPresetSettings?
    ) : Parcelable {
        fun getValue(): Double {
            return range?.getValue() ?: 0.0
        }

        fun getType(): PaymentType {
            return range?.getType() ?: PaymentType.VOLUNTARY
        }

        @Parcelize
        data class AmountRange(
            private val fixed: Double?,
            private val minimal: Double?,
            private val maximal: Double?,
        ) : Parcelable {

            fun getFixed(): Double = fixed ?: 0.0
            fun getMinimal(): Double = minimal ?: 0.0
            fun getMaximal(): Double = maximal ?: 0.0

            fun getType(): PaymentType {
                if (getFixed() > 0) return PaymentType.FIXED
                if (getMinimal() > 0) return PaymentType.MIN
                return PaymentType.VOLUNTARY
            }

            fun getValue(): Double {
                if (getFixed() > 0) return getFixed()
                if (getMinimal() > 0) return getMinimal()
                return getMaximal()
            }
        }

    }

    @Parcelize
    data class AmountPresetSettings(
        private val enabled: Boolean?,
        private val buttonAction: String?,
        private val amounts: List<Double>?
    ) : Parcelable {

        fun getEnabled() = enabled ?: false

        fun getAction() = buttonAction

        fun getValues() = amounts ?: emptyList()

    }

    @Parcelize
    data class Percents(
        private val defaultPercent: Int?,
        private val percents: List<Int>?
    ) : Parcelable {
        fun getDefault() = defaultPercent
        fun getPercents() = percents ?: emptyList()
    }

    @Parcelize
    data class AvailableFields(
        val comment: AvailableFieldsValue?,
        val name: AvailableFieldsValue?,
        val payerFeedback: AvailableFieldsValue?
    ) : Parcelable {

        @Parcelize
        data class AvailableFieldsValue(
            private val enabled: Boolean?,
            private val required: Boolean?
        ) : Parcelable {
            fun getEnabled() = enabled ?: false
            fun getRequired() = required ?: false
        }

        enum class FieldNames {
            COMMENT, NAME, FEEDBACK;
        }
    }

    @Parcelize
    data class Message(
        private var ru: String?,
        private var en: String?
    ) : Parcelable {

        fun getText(): String? {
            return ru
        }

    }

    @Parcelize
    data class Target(
        private var amount: Double?,
        private var currentAmount: Double?,
        private var finishDate: String?,
        private var startDate: String?,
        private var targetAmount: Double?
    ) : Parcelable {

        fun getAmount(): Double? {
            return amount ?: targetAmount
        }

        fun getCurrentAmount(): Double? {
            return currentAmount
        }

        fun setAmount(value: Double?) {
            targetAmount = value
        }

        fun setFinishDate(timestamp: Long?) {
            val start = Calendar.getInstance()
            val finish = timestamp?.let { Calendar.getInstance().apply { timeInMillis = it } }
            if (startDate == null) startDate = DateFormat.format(PARSE_DATE_FORMAT, start).toString()
            finishDate = finish?.let { DateFormat.format(PARSE_DATE_FORMAT, it).toString() }
        }

        @SuppressLint("SimpleDateFormat")
        fun getFinishDate(): Long? {
            return CommonHelper.stringToDate(finishDate)?.time
        }
    }

    fun getAmount(): Amount? {
        return amount
    }

    fun getPaymentValue(): Double? {
        val target = getTarget()
        if (target != null) return target.getAmount()
        return amount?.getValue()
    }

    fun getPaymentType(): PaymentType {
        if (getTarget() != null) return PaymentType.GOAL
        return amount?.getType() ?: PaymentType.VOLUNTARY
    }

    fun getTargetFinishDate(): Long? {
        return getTarget()?.getFinishDate()
    }

    fun getAvailableFields(): HashMap<AvailableFields.FieldNames, AvailableFields.AvailableFieldsValue> {
        val map = HashMap<AvailableFields.FieldNames, AvailableFields.AvailableFieldsValue>()

        availableFields?.let { fields ->
            map.apply {
                fields.name?.let {
                    put(AvailableFields.FieldNames.NAME, it)
                }
                fields.comment?.let {
                    put(AvailableFields.FieldNames.COMMENT, it)
                }
                fields.payerFeedback?.let {
                    put(AvailableFields.FieldNames.FEEDBACK, it)
                }
            }
        }
        return map
    }

    fun hasPersonalData(): Boolean {
        if (availableFields == null) return false
        if (availableFields.name != null && availableFields.name.getEnabled()) return true
        if (availableFields.payerFeedback != null && availableFields.payerFeedback.getEnabled()) return true
        return false
    }

    enum class PaymentType {
        VOLUNTARY,
        FIXED,
        MIN,
        GOAL;
    }

    companion object {

        private const val PARSE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

    }
}