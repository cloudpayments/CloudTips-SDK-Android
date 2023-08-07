package ru.cloudtips.sdk.ui.activities.tips.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.*
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import com.google.android.material.textfield.TextInputLayout
import com.yandex.pay.core.*
import com.yandex.pay.core.data.*
import com.yandex.pay.core.data.Amount
import com.yandex.pay.core.ui.YandexPayButton
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import ru.cloudtips.sdk.BuildConfig
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.card.ThreeDsDialogFragment
import ru.cloudtips.sdk.databinding.FragmentPaymentInfoBinding
import ru.cloudtips.sdk.gpay.GPayClient
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.models.PaymentInfoRatingData
import ru.cloudtips.sdk.models.RatingComponent
import ru.cloudtips.sdk.network.BasicResponse
import ru.cloudtips.sdk.network.models.PaymentAuthData
import ru.cloudtips.sdk.network.models.PaymentAuthStatusCode
import ru.cloudtips.sdk.network.models.PaymentPageData
import ru.cloudtips.sdk.ui.activities.tips.adapters.ComponentsAdapter
import ru.cloudtips.sdk.ui.activities.tips.listeners.IPaymentInfoListener
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel
import ru.cloudtips.sdk.ui.decorators.LinearHorizontalDecorator
import ru.cloudtips.sdk.ui.elements.ClickableUrlSpan
import java.text.DecimalFormat
import kotlin.math.max
import ru.cloudtips.sdk.network.models.PaymentPageData.*
import kotlin.math.roundToInt
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.helpers.PayType
import ru.cloudtips.sdk.helpers.observeOnce
import ru.cloudtips.sdk.helpers.showBottomDialog
import ru.cloudtips.sdk.models.PresetInfoData
import ru.cloudtips.sdk.ui.activities.tips.adapters.PresetsAdapter

class PaymentInfoFragment : Fragment(R.layout.fragment_payment_info), ClickableUrlSpan.ISpanUrlClick, ThreeDsDialogFragment.ThreeDSDialogListener {

    private var gPayClient: GPayClient? = null

    private var listener: IPaymentInfoListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        gPayClient = GPayClient(context)
        listener = context as? IPaymentInfoListener
        initYPayment()
    }

    override fun onDetach() {
        super.onDetach()
        gPayClient = null
        listener = null
    }

    override fun onPause() {
        super.onPause()
        requestFeeRunnable?.let {
            requestFeeHandler.removeCallbacks(it)
            requestFeeRunnable = null
        }
    }

    private val viewBinding: FragmentPaymentInfoBinding by viewBinding()
    private val viewModel: TipsViewModel by activityViewModels()

    private var paymentPageData: PaymentPageData? = null
    private var presets: PresetInfoData? = null
    private var feeAmount = 0.0

    private var ratingComponents = listOf<RatingComponent>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showSpinner()
        viewBinding.mainLayout.visibility = View.INVISIBLE

        initViews()

        viewModel.getPaymentPageData().observeOnce(viewLifecycleOwner) { data ->
            if (data != null) {
                viewBinding.mainLayout.visibility = View.VISIBLE
                hideSpinner()
                paymentPageData = data
                fillLogoView()
                fillBackground()
                fillUserData()
                fillPaymentData()
                fillInfoData()
                fillFeeAndButtonInfo()
                fillLinksInfo()
                updateGooglePayButton()
                updateYPayButton()
                updateTPayButton()
                updateSbpButton()
                amplitude.trackLayoutShow(data)
            } else {
                //TODO: show error
            }
        }

    }

    private fun initViews() = with(viewBinding) {
        feeSwitch.setOnCheckedChangeListener { _, _ ->
            updateFeeValue()
        }
        amountInputLayout.apply {
            doAfterTextChanged {
                requestFeeValue(false)
                updatePresets()
            }
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) requestFeeValue(true) }
        }

        nameInput.doAfterTextChanged { savePaymentInfoSender() }
        commentInput.doAfterTextChanged { savePaymentInfoSender() }
        feedbackInput.doAfterTextChanged { savePaymentInfoSender() }

        headerCloseButton.setOnClickListener {
            viewModel.trackPageClosed()
            listener?.onCloseClick()
        }

        cardButton.setOnClickListener {
            if (validatePayClick()) {
                lockCardButton(false)
                launchPaymentClick { requestCardClick() }
                viewModel.trackPayClick(PayType.CARD)
            }
        }
        gpayButton.setOnClickListener {
            if (validatePayClick()) {
                launchPaymentClick { requestGPayClick() }
                viewModel.trackPayClick(PayType.GOOGLEPAY)
            }
        }
        ypayButton.setOnClickListener(YandexPayButton.OnClickListener {
            if (validatePayClick()) {
                launchPaymentClick { requestYPayClick() }
                viewModel.trackPayClick(PayType.YANDEXPAY)
            }
        })
        sbpButton.setOnClickListener {
            lockSBPButton(true)
            if (validatePayClick()) {
                launchPaymentClick { requestSbpClick() }
                viewModel.trackPayClick(PayType.SBP)
            } else {
                lockSBPButton(false)
            }
        }
        tpayButton.setOnClickListener {
            lockTPayButton(true)
            if (validatePayClick()) {
                launchPaymentClick {
                    requestTPayClick()
                }
                viewModel.trackPayClick(PayType.TINKOFFPAY)
            } else {
                lockTPayButton(false)
            }
        }
    }

    private fun fillLogoView() = with(viewBinding) {
        val logo = paymentPageData?.getLogo()
        Glide.with(logoView).load(logo).error(R.drawable.ic_logo_horizontal).fitCenter().into(logoView)
        CommonHelper.setViewTint(headerCloseButton, getButtonsColor())
    }

    private fun launchPaymentClick(callback: () -> Unit) {
        CommonHelper.hideKeyboard(view)
        val amount = getAmount()
        viewModel.getFeeValue(amount).observe(viewLifecycleOwner) {
            callback.invoke()
        }
        view?.clearFocus()
    }

    private fun validatePayClick(): Boolean {
        return if (isValid()) {
            savePaymentInfoAmount()
            savePaymentInfoSender()
            savePaymentInfoRating()
            true
        } else false
    }

    private fun savePaymentInfoAmount() = with(viewBinding) {
        viewModel.putPaymentInfoAmountData(getAmount(), feeAmount, feeSwitch.isChecked)
    }

    private fun savePaymentInfoSender() = with(viewBinding) {
        val name = nameInput.text?.toString()
        val comment = commentInput.text?.toString()
        val feedback = feedbackInput.text?.toString()
        viewModel.putPaymentInfoSenderData(name, comment, feedback)
    }

    private fun savePaymentInfoRating() = with(viewBinding) {
        val rating = rating
        val ratingComponents = ratingComponents.filter { it.selected }
        viewModel.putPaymentInfoRatingData(rating, ratingComponents)
    }

    private fun lockCardButton(enable: Boolean) = with(viewBinding) {
        cardButton.visibility = if (enable) View.VISIBLE else View.GONE
        cardBlockButton.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private fun requestCardClick() {
        lockCardButton(true)
        requireActivity().showBottomDialog(PaymentCardBottomFragment.newInstance())
    }

    private fun updateGooglePayButton() = with(viewBinding) {
        if (paymentPageData?.getGooglePayEnabled() == true) {
            gPayClient?.canUseGooglePay?.observe(viewLifecycleOwner) {
                gpayButton.visibility = if (it) View.VISIBLE else View.GONE
            }
        } else {
            gpayButton.visibility = View.GONE
        }
    }

    private fun requestGPayClick() = with(viewBinding) {
        // Disables the button to prevent multiple clicks.
        gpayButton.isClickable = false
        viewModel.getMerchantId().observeOnce(viewLifecycleOwner) { publicData ->
            viewModel.getPaymentInfoData().observeOnce(viewLifecycleOwner) {
                gPayClient?.getLoadPaymentDataTask(it.getAmountWithFee(), publicData?.publicId)?.addOnCompleteListener { completedTask ->
                    if (completedTask.isSuccessful) {
                        launchGPayment(completedTask.result)
                    } else {
                        when (val exception = completedTask.exception) {
                            is ResolvableApiException -> {
                                resolveGPaymentForResult.launch(
                                    IntentSenderRequest.Builder(exception.resolution).build()
                                )
                            }
                            is ApiException -> {
                                handleGPayError(exception.statusCode, exception.message)
                            }
                            else -> {
                                handleGPayError(
                                    CommonStatusCodes.INTERNAL_ERROR, "Unexpected non API" +
                                            " exception when trying to deliver the task result to an activity!"
                                )
                            }
                        }
                    }

                    // Re-enables the Google Pay payment button.
                    gpayButton.isClickable = true
                }
            }
        }
    }

    private val resolveGPaymentForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        when (result.resultCode) {
            AppCompatActivity.RESULT_OK ->
                result.data?.let { intent ->
                    launchGPayment(PaymentData.getFromIntent(intent))
                }

            AppCompatActivity.RESULT_CANCELED -> {
                // The user cancelled the payment attempt
            }
        }
    }

    private fun handleGPayError(statusCode: Int, message: String?) {
        Log.e("Google Pay API error", "Error code: $statusCode, Message: $message")
        onPaymentFailure(PayType.GOOGLEPAY)
    }

    private fun launchGPayment(paymentData: PaymentData?) {
        showSpinner()
        viewModel.launchGPayment(paymentData).observe(viewLifecycleOwner) { response ->
            hideSpinner()
            handlePaymentResponse(response, PayType.GOOGLEPAY)
        }
    }

    private fun handlePaymentResponse(response: BasicResponse<PaymentAuthData>, payType: PayType) {
        val data = response.data
        when (data?.getStatusCode()) {
            PaymentAuthStatusCode.NEED3DS -> {
                val fragment3ds = ThreeDsDialogFragment.newInstance(data, payType)
                fragment3ds.show(parentFragmentManager, "NEED3DS")
                fragment3ds.setTargetFragment(this@PaymentInfoFragment, REQUEST_CODE_3DS)
            }
            PaymentAuthStatusCode.SUCCESS -> {
                onPaymentSuccess(payType)
            }
            else -> {
                onPaymentFailure(payType)
            }
        }
    }

    private fun onPaymentSuccess(payType: PayType) {
        listener?.onPaymentSuccess(payType)
    }

    private fun onPaymentFailure(payType: PayType) {
        listener?.onPaymentFailure(payType)
    }

    private fun updateYPayButton() = with(viewBinding) {
        val enabled = YandexPayLib.isSupported
        ypayButton.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun initYPayment() {
        if (YandexPayLib.isSupported) {
            val environment = if (BuildConfig.DEBUG) YandexPayEnvironment.SANDBOX else YandexPayEnvironment.PROD
            val logging = BuildConfig.DEBUG
            YandexPayLib.initialize(
                requireContext(), YandexPayLibConfig(
                    environment = environment,
                    logging = logging,
                    locale = YandexPayLocale.RU,
                    merchantDetails = Merchant(
                        id = MerchantId.from(getString(R.string.ypay_merchant_id)),
                        name = getString(R.string.ypay_merchant_name),
                        url = getString(R.string.ypay_merchant_url)
                    )
                )
            )
        }
    }

    private fun requestYPayClick() = with(viewBinding) {
        ypayButton.isClickable = false
        viewModel.getMerchantId().observeOnce(viewLifecycleOwner) {
            val publicId = it?.publicId ?: ""
            viewModel.getPaymentInfoData().observeOnce(viewLifecycleOwner) { infoData ->
                val name = if (!nameTextView.text.isNullOrEmpty()) nameTextView.text.toString() else "CloudTips"
                val amount = infoData.getAmountWithFee()
                yandexPayLauncher.launch(
                    OrderDetails(
                        order = Order(
                            id = OrderID.from(name),
                            amount = Amount.from(amount.toString()),
                            label = name,
                            listOf()
                        ),
                        paymentMethods = listOf(
                            PaymentMethod(
                                allowedAuthMethods = listOf(AuthMethod.PanOnly),
                                type = PaymentMethodType.Card,
                                gateway = Gateway.from("cloudpayments"),
                                allowedCardNetworks = listOf(CardNetwork.Visa, CardNetwork.MasterCard, CardNetwork.MIR),
                                gatewayMerchantId = GatewayMerchantID.from(publicId),
                            )
                        )
                    )
                )
            }
        }
    }

    private val yandexPayLauncher = registerForActivityResult(OpenYandexPayContract()) { result: YandexPayResult ->
        viewBinding.ypayButton.isClickable = true
        when (result) {
            is YandexPayResult.Success -> handleYPaySuccess(result.paymentToken)
            is YandexPayResult.Failure -> when (result) {
                is YandexPayResult.Failure.Validation -> handleYPayFailure(result.details.name)
                is YandexPayResult.Failure.Internal -> handleYPayFailure(result.message)
            }
            YandexPayResult.Cancelled -> {}
        }
    }

    private fun handleYPaySuccess(paymentToken: PaymentToken) {
        val token = String(Base64.decode(paymentToken.toString(), Base64.DEFAULT))
        showSpinner()
        viewModel.launchYPayment(token).observeOnce(viewLifecycleOwner) { response ->
            hideSpinner()
            handlePaymentResponse(response, PayType.YANDEXPAY)
        }
    }

    private fun handleYPayFailure(message: String?) {
        Log.w("loadPaymentData failed", String.format("Ya payment error: %s", message))
        onPaymentFailure(PayType.YANDEXPAY)
    }

    private fun lockSBPButton(locked: Boolean) = with(viewBinding) {
        sbpButton.visibility = if (locked) View.GONE else View.VISIBLE
        sbpBlockButton.visibility = if (locked) View.VISIBLE else View.GONE
    }

    private fun updateSbpButton() = with(viewBinding) {
        val available = paymentPageData?.getSbpEnabled() == true
        sbpLayout.visibility = if (available) View.VISIBLE else View.GONE
    }

    private fun requestSbpClick() {
        listener?.onPayInfoSbpLaunch()
        lockSBPButton(false)
    }

    private fun updateTPayButton() = with(viewBinding) {
        val enabled = (paymentPageData?.getTinkoffPayEnabled() ?: false) && CommonHelper.isTinkoffBankAvailable(requireContext())
        tpayLayout.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun lockTPayButton(locked: Boolean) = with(viewBinding) {
        tpayButton.visibility = if (locked) View.GONE else View.VISIBLE
        tpayBlockButton.visibility = if (locked) View.VISIBLE else View.GONE
    }

    private fun requestTPayClick() {
        lockTPayButton(true)
        viewModel.launchTPayment().observeOnce(viewLifecycleOwner) {
            lockTPayButton(false)
            if (it.succeed) {
                listener?.onPayInfoTinkoffLaunch(it.data)
            } else {
                onPaymentFailure(PayType.TINKOFFPAY)
            }
        }
    }

    override fun onAuthorizationCompleted(md: String, paRes: String, payType: PayType) {
        showSpinner()
        viewModel.postPayment3ds(md, paRes).observeOnce(this) {
            hideSpinner()
            handlePaymentResponse(it, payType)
        }

    }

    override fun onAuthorizationFailed(error: String?, payType: PayType) {
        onPaymentFailure(payType)
    }


    private fun isValid(): Boolean {
        return isValidAmount() && isValidFields() && isValidRating()
    }

    private fun isValidAmount(): Boolean = with(viewBinding) {
        val data = paymentPageData ?: return false
        val amount = getAmount()
        val result = when (data.getPaymentType()) {
            PaymentType.FIXED -> {
                true
            }
            PaymentType.MIN,
            PaymentType.VOLUNTARY -> {
                val minAmount = paymentPageData?.getPaymentValue() ?: 0.0
                val maxAmount = paymentPageData?.getAmount()?.range?.getMaximal() ?: 0.0

                amount in minAmount..maxAmount
            }
            PaymentType.GOAL -> {
                val maxAmount = paymentPageData?.getAmount()?.range?.getMaximal() ?: 0.0
                val minAmount = paymentPageData?.getAmount()?.range?.getMinimal() ?: 0.0

                amount in minAmount..maxAmount
            }
        }

        amountInputLayout.helperText = if (!result) getString(R.string.field_amount_incorrect) else null

        if (!result) {
            scrollToView(amountInputLayout)
            amountInputLayout.requestFocus()
        }

        return result
    }

    private fun scrollToView(view: View) = with(viewBinding) {
        var dY = view.top
        var parent = view.parent
        while (parent != null && parent != mainLayout) {
            dY += (parent as? View)?.top ?: 0
            parent = parent.parent
        }
        mainLayout.scrollTo(0, dY)
    }

    private fun getAmount(): Double {
        val data = paymentPageData ?: return 0.0

        return when (data.getPaymentType()) {
            PaymentType.FIXED -> data.getPaymentValue() ?: 0.0
            else -> viewBinding.amountInputLayout.getText()?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun setAmount(value: Double) = with(viewBinding) {
        if (value > 0) amountInputLayout.setText(CommonHelper.formatDouble(value))
    }

    private fun addAmount(additionalAmount: Double) = with(viewBinding) {
        val currentAmount = amountInputLayout.getText()?.toDoubleOrNull() ?: 0.0
        val newAmount = currentAmount + additionalAmount
        amountInputLayout.setText(CommonHelper.formatDouble(newAmount))
    }

    private fun updatePresets() {
        val selectedPreset = presets?.getSelectedIndexBySum(getAmount())
        if (selectedPreset != null) {
            (viewBinding.bubbleLayout.adapter as? PresetsAdapter)?.setSelectedValue(selectedPreset)
        }
    }

    private fun isValidFields(): Boolean = with(viewBinding) {
        val data = paymentPageData ?: return false
        val fields = data.getAvailableFields()

        return isValidField(fields[AvailableFields.FieldNames.NAME], nameInputLayout) &&
                isValidField(fields[AvailableFields.FieldNames.COMMENT], commentInputLayout) &&
                isValidField(fields[AvailableFields.FieldNames.FEEDBACK], feedbackInputLayout)
    }

    private fun isValidField(field: AvailableFields.AvailableFieldsValue?, formField: TextInputLayout): Boolean {
        if (field == null) return true
        val value = formField.editText?.text?.toString()
        val isValid = !field.getRequired() || !value.isNullOrEmpty()
        formField.helperText = if (!isValid) getString(R.string.field_required) else null
        if (!isValid) formField.requestFocus()
        return isValid
    }

    private fun isValidRating(): Boolean {
        return true
    }

    private fun fillBackground() = with(viewBinding) {
        val background = paymentPageData?.getBackground()
        if (background.isNullOrEmpty()) {
            backgroundImageLayout.visibility = View.GONE
        } else {
            backgroundImageLayout.visibility = View.VISIBLE
            Glide.with(backgroundImageView).load(background).centerCrop().into(backgroundImageView)
        }
        val backgroundColor = paymentPageData?.getBackgroundColor()
        if (backgroundColor != null) {
            backgroundLayout.setBackgroundColor(backgroundColor)
            backgroundImageGradient.setColorFilter(backgroundColor)
        }

    }

    private fun fillUserData() = with(viewBinding) {
        val data = paymentPageData ?: return@with
        if (!data.getUserName().isNullOrEmpty()) {
            nameTextView.visibility = View.VISIBLE
            nameTextView.text = data.getUserName()
        } else nameTextView.visibility = View.GONE
        if (!data.getPaymentMessage().isNullOrEmpty()) nameMessageView.text = data.getPaymentMessage()
        Glide.with(avatarView).load(data.getUserAvatar())
            .placeholder(R.drawable.ic_empty_avatar)
            .error(R.drawable.ic_empty_avatar)
            .transform(
                CenterCrop(),
                RoundedCornersTransformation(
                    resources.getDimensionPixelSize(R.dimen.profile_avatar_big_radius),
                    0
                )
            ).into(avatarView)

    }

    private fun fillPaymentData() = with(viewBinding) {
        val data = paymentPageData ?: return@with
        when (data.getPaymentType()) {
            PaymentType.FIXED -> {
                priceInputLayout.visibility = View.GONE
                fixedPriceLayout.visibility = View.VISIBLE
                fixedPriceView.text = getString(R.string.main_balance, data.getPaymentValue()?.roundToInt())
                requestFeeValue(true)
            }
            PaymentType.MIN,
            PaymentType.VOLUNTARY -> {
                priceInputLayout.visibility = View.VISIBLE
                fixedPriceLayout.visibility = View.GONE

                priceGoalLayout.visibility = View.GONE
                fillPrice(data)
            }
            PaymentType.GOAL -> {
                priceInputLayout.visibility = View.VISIBLE
                fixedPriceLayout.visibility = View.GONE

                priceGoalLayout.visibility = View.VISIBLE

                fillGoal(data.getTarget())
                fillPrice(data)
            }
        }
    }

    private fun fillGoal(target: PaymentPageData.Target?) = with(viewBinding) {
        if (target == null) return@with
        val maxValue = (target.getAmount() ?: 0.0).roundToInt()
        val curValue = (target.getCurrentAmount() ?: 0.0).roundToInt()
        val leftValue = max(maxValue - curValue, 0)
        goalTargetView.text = getString(R.string.main_balance, maxValue)
        goalCurrentView.text = getString(R.string.main_balance, leftValue)
        val progress = if (maxValue > 0) (100 * curValue / maxValue) else 0
        goalProgressbarView.progress = progress
        CommonHelper.setViewTint(goalProgressbarView, getButtonsColor())
    }

    private fun fillPrice(paymentPageData: PaymentPageData?) = with(viewBinding) {
        val defaultMin = 0.0
        val defaultMax = 0.0

        val userMin = paymentPageData?.getAmount()?.range?.getMinimal() ?: 0.0
        val userMax = paymentPageData?.getAmount()?.range?.getMaximal() ?: 0.0
        val minValue = if (userMin > 1e-6) userMin else defaultMin
        val maxValue = if (userMax > 1e-6) userMax else defaultMax

        priceInputView.text = getString(R.string.link_edit_payment_page_set_price, minValue.toInt(), maxValue.toInt())
        viewModel.getSum().observeOnce(viewLifecycleOwner) { sumData ->
            val sum = sumData ?: 0.0
            if (sum > 0 && paymentPageData?.getTarget() == null) {
                amountSumTextView.visibility = View.VISIBLE
                amountSumTextView.text = getString(R.string.link_edit_payment_sum, CommonHelper.formatDouble(sum))
            } else {
                amountSumTextView.visibility = View.GONE
            }
            fillPresets(sum)
        }
    }

    private fun fillPresets(sum: Double) = with(viewBinding) {
        viewModel.getPresetSettings().observeOnce(viewLifecycleOwner) { presets ->
            this@PaymentInfoFragment.presets = presets
            if (presets != null && presets.values.isNotEmpty()) {
                bubbleLayout.visibility = View.VISIBLE
                bubbleLayout.apply {
                    if (itemDecorationCount == 0) addItemDecoration(
                        LinearHorizontalDecorator(
                            resources.getDimensionPixelSize(R.dimen.bubble_outer_spacing),
                            resources.getDimensionPixelSize(R.dimen.bubble_inner_spacing)
                        )
                    )
                    adapter = PresetsAdapter(presets.values, getButtonsColor(), object : PresetsAdapter.Listener {
                        override fun onPresetClicked(item: PresetInfoData.Preset) {
                            when (presets.type) {
                                PresetInfoData.Type.Add -> addAmount(item.value)
                                PresetInfoData.Type.Value -> setAmount(item.value)
                                PresetInfoData.Type.Percent -> setAmount(item.value)
                            }
                        }
                    })

                }
                val presetSum = paymentPageData?.getPresetSum(sum) ?: 0.0
                if (presetSum > 0) setAmount(presetSum)
            } else {
                bubbleLayout.visibility = View.GONE
            }
        }

    }

    private fun fillInfoData() = with(viewBinding) {
        viewModel.getPaymentInfoData().observe(viewLifecycleOwner) { infoData ->
            val data = paymentPageData ?: return@observe
            setAmount(infoData.getAmount())
            val fields = data.getAvailableFields()
            fields[AvailableFields.FieldNames.NAME]?.let { field ->
                nameInputLayout.visibility = if (field.getEnabled()) View.VISIBLE else View.GONE
                if (nameInput.text?.toString() != infoData?.sender?.name)
                    nameInput.setText(infoData?.sender?.name)
            }
            fields[AvailableFields.FieldNames.COMMENT]?.let { field ->
                commentInputLayout.visibility = if (field.getEnabled()) View.VISIBLE else View.GONE
                if (commentInput.text?.toString() != infoData?.sender?.comment)
                    commentInput.setText(infoData?.sender?.comment)
            }
            fields[AvailableFields.FieldNames.FEEDBACK]?.let { field ->
                feedbackInputLayout.visibility = if (field.getEnabled()) View.VISIBLE else View.GONE
                if (feedbackInput.text?.toString() != infoData?.sender?.feedback)
                    feedbackInput.setText(infoData?.sender?.feedback)
            }
            fillRating(infoData?.rating)
        }
    }

    private fun fillRating(ratingInfoData: PaymentInfoRatingData?) = with(viewBinding) {
        val ratingData = paymentPageData?.getRating()
        if (ratingData == null || !ratingData.getEnabled()) {
            ratingLayout.visibility = View.GONE
            return@with
        }
        ratingLayout.visibility = View.VISIBLE
        ratingStarsTitle.text = resources.getString(R.string.link_edit_payment_page_do_you_like, ratingData.getStarsText())
        ratingImage1.setOnClickListener { setRating(1) }
        ratingImage2.setOnClickListener { setRating(2) }
        ratingImage3.setOnClickListener { setRating(3) }
        ratingImage4.setOnClickListener { setRating(4) }
        ratingImage5.setOnClickListener { setRating(5) }

        ratingComponents = (ratingData.getComponents() ?: emptyList()).map {
            RatingComponent(it.id, it.title, it.imageUrl).apply {
                selected = ratingInfoData?.components?.contains(it.id) ?: false
            }
        }

        hideRatingComponents()
        if (ratingComponents.isNotEmpty()) {
            ratingComponentsTitle.text = ratingData.getComponentsText()
            ratingComponentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                if (itemDecorationCount == 0)
                    addItemDecoration(
                        LinearHorizontalDecorator(
                            resources.getDimensionPixelSize(R.dimen.components_outer_spacing),
                            resources.getDimensionPixelSize(R.dimen.components_inner_spacing)
                        )
                    )
                adapter = ComponentsAdapter(ratingComponents, getButtonsColor(), object : ComponentsAdapter.IComponentsAdapterListener {
                    override fun onItemClicked(item: RatingComponent) {
                        changeRatingComponent(item)
                    }

                })
            }
        }
        setRating(ratingInfoData?.score ?: 0)
    }

    private fun changeRatingComponent(item: RatingComponent) {
        val index = ratingComponents.indexOf(item)
        if (index < 0) return
        ratingComponents[index].selected = !ratingComponents[index].selected
        viewBinding.ratingComponentsRecyclerView.adapter?.notifyItemChanged(index)

        viewModel.putPaymentInfoRatingData(rating, ratingComponents)
    }

    private var rating: Int = 0
    private fun setRating(value: Int) = with(viewBinding) {
        rating = value
        ratingImage1.setImageResource(if (value >= 1) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        ratingImage2.setImageResource(if (value >= 2) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        ratingImage3.setImageResource(if (value >= 3) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        ratingImage4.setImageResource(if (value >= 4) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        ratingImage5.setImageResource(if (value >= 5) R.drawable.ic_star_filled else R.drawable.ic_star_empty)
        if (rating > 3) showRatingComponents()
        else hideRatingComponents()

        viewModel.putPaymentInfoRatingData(rating, ratingComponents)
    }

    private fun showRatingComponents() = with(viewBinding) {
        ratingComponentsTitle.visibility = View.VISIBLE
        ratingComponentsRecyclerView.visibility = View.VISIBLE
    }

    private fun hideRatingComponents() = with(viewBinding) {
        ratingComponentsTitle.visibility = View.GONE
        ratingComponentsRecyclerView.visibility = View.GONE
    }

    private fun fillFeeAndButtonInfo() = with(viewBinding) {
        feeSwitch.isChecked = paymentPageData?.payerFee?.getIsEnabled() ?: false
        feeLayout.visibility = if (paymentPageData?.payerFee?.getIsEnabled() != false) View.VISIBLE else View.GONE
        val buttonColor = getButtonsColor()
        CommonHelper.setViewTint(feeSwitch, buttonColor)
        CommonHelper.setViewTint(cardButton, buttonColor)
        CommonHelper.setViewTint(cardBlockButton, buttonColor)
        updateFeeValue()
    }

    private fun isFeeVisible(): Boolean {
        return paymentPageData?.payerFee?.getIsEnabled() ?: false
    }

    private val requestFeeHandler = Handler()
    private var requestFeeRunnable: Runnable? = null
    private fun requestFeeValue(immediately: Boolean) {
        if (!isFeeVisible()) return
        viewBinding.amountInputLayout.helperText = null
        requestFeeRunnable?.let { requestFeeHandler.removeCallbacks(it) }
        requestFeeRunnable = Runnable {
            val amount = getAmount()
            viewModel.getFeeValue(amount).observe(viewLifecycleOwner) {
                feeAmount = it
                updateFeeValue()
                savePaymentInfoAmount()
            }
        }
        val delay = if (immediately) 0L else 1000L
        requestFeeRunnable?.let { requestFeeHandler.postDelayed(it, delay) }
    }

    private fun updateFeeValue() = with(viewBinding) {
        val formattedValue = DecimalFormat("#.#").format(feeAmount)
        feeHint.text = getString(R.string.link_edit_payment_page_tips_info_subtitle, formattedValue)
    }

    private fun fillLinksInfo() = with(viewBinding) {
        val linksColor = paymentPageData?.getLinksColor()
        val hasPersonalData = paymentPageData?.hasPersonalData() ?: false
        val spannedLicense: SpannableStringBuilder
        if (!hasPersonalData) {
            val licenseUrl = getString(R.string.license_url)
            val licenseSub = getString(R.string.link_edit_payment_page_info_text_subs_license)
            val licenseText = getString(R.string.link_edit_payment_page_info_text, licenseSub)
            val licenseStart = licenseText.indexOf(licenseSub)
            spannedLicense = SpannableStringBuilder(licenseText)
            spannedLicense.setSpan(
                ClickableUrlSpan(licenseUrl, this@PaymentInfoFragment, true, linksColor),
                licenseStart,
                licenseStart + licenseSub.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        } else {
            val licenseUrl = getString(R.string.license_url)
            val agreementUrl = getString(R.string.agreement_url)
            val policyUrl = getString(R.string.policy_url)
            val licenseSub = getString(R.string.link_edit_payment_page_info_text_subs_license)
            val agreementSub = getString(R.string.link_edit_payment_page_info_text_subs_agreement)
            val policySub = getString(R.string.link_edit_payment_page_info_text_subs_policy)
            val licenseText = getString(R.string.link_edit_payment_page_info_full_text, licenseSub, agreementSub, policySub)
            val licenseStart = licenseText.indexOf(licenseSub)
            val agreementStart = licenseText.indexOf(agreementSub)
            val policyStart = licenseText.indexOf(policySub)
            spannedLicense = SpannableStringBuilder(licenseText)
            spannedLicense.setSpan(
                ClickableUrlSpan(licenseUrl, this@PaymentInfoFragment, true, linksColor),
                licenseStart,
                licenseStart + licenseSub.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            spannedLicense.setSpan(
                ClickableUrlSpan(agreementUrl, this@PaymentInfoFragment, true, linksColor),
                agreementStart,
                agreementStart + agreementSub.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            spannedLicense.setSpan(
                ClickableUrlSpan(policyUrl, this@PaymentInfoFragment, true, linksColor),
                policyStart,
                policyStart + policySub.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        licenseTextView.text = spannedLicense
        licenseTextView.movementMethod = LinkMovementMethod.getInstance()

        val privacyUrl = getString(R.string.google_captch_privacy)
        val termsUrl = getString(R.string.google_captch_terms)
        val privacySub = getString(R.string.link_edit_payment_page_captcha_text_subs_privacy)
        val termsSub = getString(R.string.link_edit_payment_page_captcha_text_subs_terms)
        val captchaText = getString(R.string.link_edit_payment_page_captcha_text, privacySub, termsSub)
        val privacyStart = captchaText.indexOf(privacySub)
        val termsStart = captchaText.indexOf(termsSub)
        val spannedCaptcha = SpannableStringBuilder(captchaText)
        spannedCaptcha.setSpan(
            ClickableUrlSpan(privacyUrl, this@PaymentInfoFragment, true, linksColor),
            privacyStart,
            privacyStart + privacySub.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannedCaptcha.setSpan(
            ClickableUrlSpan(termsUrl, this@PaymentInfoFragment, true, linksColor),
            termsStart,
            termsStart + termsSub.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        captchaTextView.text = spannedCaptcha
        captchaTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onUrlClick(url: String?) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        requireContext().let {
            if (intent.resolveActivity(it.packageManager) != null) {
                it.startActivity(intent)
            }
        }
    }

    private fun showSpinner() = with(viewBinding) {
        spinnerLayout.root.visibility = View.VISIBLE
    }

    private fun hideSpinner() = with(viewBinding) {
        spinnerLayout.root.visibility = View.GONE
    }

    private fun getButtonsColor(): Int {
        return paymentPageData?.getButtonsColor() ?: requireContext().getColor(R.color.colorAccent)
    }

    companion object {
        private const val REQUEST_CODE_3DS = 1001

        fun newInstance() = PaymentInfoFragment()

    }

}