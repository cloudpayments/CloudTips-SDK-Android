package ru.cloudtips.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.util.TextWatcherAdapter
import ru.cloudtips.sdk.CloudTipsSDK
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.TipsConfiguration
import ru.cloudtips.sdk.api.Api
import ru.cloudtips.sdk.api.ApiEndPoint
import ru.cloudtips.sdk.api.models.*
import ru.cloudtips.sdk.base.PayActivity
import ru.cloudtips.sdk.databinding.ActivityTipsBinding
import ru.cloudtips.sdk.utils.GooglePayHandler
import java.text.DecimalFormat

internal class TipsActivity : PayActivity() {

    companion object {
        private const val REQUEST_CODE_CARD_ACTIVITY = 101
        private const val REQUEST_CODE_GOOGLE_PAY = 102

        private const val EXTRA_CONFIGURATION = "EXTRA_CONFIGURATION"

        fun getStartIntent(context: Context, configuration: TipsConfiguration): Intent {
            val intent = Intent(context, TipsActivity::class.java)
            intent.putExtra(EXTRA_CONFIGURATION, configuration)
            return intent
        }
    }

    private val configuration by lazy {
        intent.getParcelableExtra<TipsConfiguration>(EXTRA_CONFIGURATION)
    }

    private var photoUrl = ""
    private var name = ""

    private var minAmount = 49
    private var maxAmount = 10000

    private lateinit var binding: ActivityTipsBinding

    private lateinit var layoutId: String

    private lateinit var gPayToken: String

    override fun showLoading() {
        binding.loading.visibility = View.VISIBLE
        binding.content.visibility = View.GONE
    }

    override fun hideLoading() {
        binding.loading.visibility = View.GONE
        binding.content.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ApiEndPoint.testMode = configuration.testMode

        showLoading()
        initUI()
    }

    private fun initUI() {

        binding.imageViewClose.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra(CloudTipsSDK.IntentKeys.TransactionStatus.name, CloudTipsSDK.TransactionStatus.Cancelled)
            })
            finish()
        }

        binding.editTextAmount.addTextChangedListener(object : TextWatcherAdapter() {

            override fun afterTextChanged(s: Editable?) {
                super.afterTextChanged(s)

                errorMode(false, binding.editTextAmount)
                binding.textViewAmountDesc.setTextColor(ContextCompat.getColor(this@TipsActivity, R.color.not_error))

                val amount = s.toString()

                if (amount != "100") binding.radioButton100.isChecked = false
                if (amount != "200") binding.radioButton200.isChecked = false
                if (amount != "300") binding.radioButton300.isChecked = false
                if (amount != "500") binding.radioButton500.isChecked = false
                if (amount != "1000") binding.radioButton1000.isChecked = false
                if (amount != "2000") binding.radioButton2000.isChecked = false
                if (amount != "3000") binding.radioButton3000.isChecked = false
                if (amount != "5000") binding.radioButton5000.isChecked = false

                requestFeeValue(false)
            }
        })
        //update fee on leave focus
        binding.editTextAmount.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) requestFeeValue(true)
        }

        val listener = CompoundButton.OnCheckedChangeListener() { buttonView, isChecked ->
            (buttonView as RadioButton).also {
                if (isChecked) {
                    binding.editTextAmount.setText(it.text.toString().replace(" ₽", ""))
                    requestFeeValue(true)
                }
            }
        }

        binding.radioButton100.setOnCheckedChangeListener(listener)
        binding.radioButton200.setOnCheckedChangeListener(listener)
        binding.radioButton300.setOnCheckedChangeListener(listener)
        binding.radioButton500.setOnCheckedChangeListener(listener)
        binding.radioButton1000.setOnCheckedChangeListener(listener)
        binding.radioButton2000.setOnCheckedChangeListener(listener)
        binding.radioButton3000.setOnCheckedChangeListener(listener)
        binding.radioButton5000.setOnCheckedChangeListener(listener)

        GooglePayHandler.isReadyToMakeGooglePay(this)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                if (it) {
                    binding.buttonGPay.visibility = View.VISIBLE
                } else {
                    binding.buttonGPay.visibility = View.GONE
                }
                getLayout(configuration.tipsData.phone)
            }
            .onErrorReturn {
                binding.buttonGPay.visibility = View.GONE
                getLayout(configuration.tipsData.phone)
            }
            .subscribe()

        binding.buttonPayCard.setOnClickListener {

            if (!isValid()) {
                return@setOnClickListener
            }

            val intent = CardActivity.getStartIntent(this, photoUrl, name, layoutId, amount(), getAmountWithFee(), comment(), feeFromPayer())
            startActivityForResult(intent, REQUEST_CODE_CARD_ACTIVITY)
        }

        binding.buttonGPay.setOnClickListener {

            if (!isValid()) {
                return@setOnClickListener
            }

            getPublicIdForGPay(layoutId)
        }

        val text = getString(R.string.tips_agreement_first) + " <u>" + getString(
            R.string.tips_agreement_second
        ) + "</u>"
        binding.textViewAgreement.text = Html.fromHtml(text)

        binding.textViewAgreement.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://static.cloudpayments.ru/docs/cloudtips_oferta.pdf"))
            startActivity(intent)
        }

        initRecaptchaTextView(binding.textViewRecaptcha)
    }

    private fun getLayout(phoneNumber: String) {
        compositeDisposable.add(
            Api.getLayout(phoneNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ layouts -> checkGetLayoutResponse(layouts) }, this::handleError)
        )
    }

    private fun checkGetLayoutResponse(layouts: ArrayList<Layout>) {
        if (layouts.size == 0) {
            offlineRegister(configuration.tipsData.phone, configuration.tipsData.name, configuration.tipsData.partner)
        } else {
            layouts[0].layoutId?.let {
                layoutId = it
                getPaymentPage(layoutId)
            }
        }
    }

    private fun offlineRegister(phoneNumber: String, name: String, partner: String) {
        compositeDisposable.add(
            Api.offlineRegister(phoneNumber, name, partner)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ layouts -> checkOfflineRegisterResponse(layouts) }, this::handleError)
        )
    }

    private fun checkOfflineRegisterResponse(layouts: ArrayList<Layout>) {
        if (layouts.size == 0) {
            showToast(R.string.app_error)
            finish()
        } else {
            layouts[0].layoutId?.let {
                layoutId = it
                getPaymentPage(layoutId)
            }
        }
    }

    private fun getPaymentPage(layoutId: String) {

        compositeDisposable.add(
            Api.getPaymentPage(layoutId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ paymentPage -> checkGetPaymentPageResponse(paymentPage) }, this::handleError)
        )
    }

    private fun checkGetPaymentPageResponse(paymentPage: PaymentPage) {

        photoUrl = paymentPage.avatarUrl ?: ""
        if (photoUrl == "https://api.cloudtips.ru/api/images/avatar-default" || photoUrl == "https://api-sandbox.cloudtips.ru/api/images/avatar-default") {
            photoUrl = ""
        }

        if (photoUrl != "") {

            Glide
                .with(this)
                .load(paymentPage.avatarUrl)
                .apply(RequestOptions.bitmapTransform(CenterCrop()))
                .circleCrop()
                .into(binding.imageViewAvatar)
        }

        name = paymentPage.nameText ?: ""

        if (name.isEmpty()) {
            binding.textViewName.visibility = View.GONE
            binding.textViewDescription.setText(R.string.tips_desc_name_is_empty)
        } else {
            binding.textViewName.visibility = View.VISIBLE
            binding.textViewName.text = name
            binding.textViewDescription.setText(R.string.tips_desc_name_is_not_empty)
        }

        val amountRange = paymentPage.amount?.range
        if (amountRange != null) {

            if (amountRange.minimal != null) {
                minAmount = amountRange.minimal.toInt()
            }

            if (amountRange.maximal != null) {
                maxAmount = amountRange.maximal.toInt()
            }
        }

        val dec = DecimalFormat("#,###.##")
        val amount_desc =
            getString(R.string.tips_amount_desc_start) + dec.format(minAmount) + getString(R.string.tips_amount_desc_divider) + dec.format(maxAmount) + getString(
                R.string.tips_amount_desc_end
            )
        binding.textViewAmountDesc.text = amount_desc

        feeVisible = paymentPage.payerFee?.enabled ?: false
        val feeEnabled = paymentPage.payerFee?.getIsEnabled() ?: false
        binding.feeLayout.visibility = if (feeVisible) View.VISIBLE else View.GONE
        binding.feeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            updateFeeValue()
        }
        binding.feeSwitch.isChecked = feeEnabled
        updateFeeValue()

        hideLoading()
    }

    private var feeVisible = false
    private var feeAmount: Double = 0.0
    private val requestFeeHandler = Handler()
    private var requestFeeRunnable: Runnable? = null
    private fun requestFeeValue(immediately: Boolean) {
        if (!feeVisible) return
        requestFeeRunnable?.let { requestFeeHandler.removeCallbacks(it) }
        requestFeeRunnable = Runnable {
            val amount = amount().toDoubleOrNull() ?: 0.0
            compositeDisposable.add(
                Api.getPaymentFee(layoutId, amount)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ fee ->
                        feeAmount = fee.amountFromPayer ?: 0.0
                        updateFeeValue()
                    }, this::handleError)
            )
        }
        val delay = if (immediately) 0L else 1000L
        requestFeeHandler.postDelayed(requestFeeRunnable, delay)
    }

    private fun updateFeeValue() {
        val formattedValue = DecimalFormat("#.#").format(feeAmount)
        binding.feeText.text = getString(R.string.tips_fee_description, formattedValue)
    }

    private fun getAmountWithFee(): Double {
        return if (binding.feeSwitch.isChecked) feeAmount else amount().toDoubleOrNull() ?: 0.0
    }

    private fun getPublicIdForGPay(layoutId: String) {
        showLoading()
        compositeDisposable.add(
            Api.getPublicId(layoutId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ publicId -> checkGetPublicIdForGPayResponse(publicId) }, this::handleError)
        )
    }

    private fun checkGetPublicIdForGPayResponse(publicId: PublicId) {
        val publicId = publicId.publicId.toString()
        hideLoading()
        GooglePayHandler.present(publicId, amount(), this, REQUEST_CODE_GOOGLE_PAY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) = when (requestCode) {
        REQUEST_CODE_CARD_ACTIVITY -> {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    setResult(RESULT_OK, Intent().apply {
                        val transactionStatus =
                            data?.getSerializableExtra(CloudTipsSDK.IntentKeys.TransactionStatus.name) as? CloudTipsSDK.TransactionStatus
                        putExtra(CloudTipsSDK.IntentKeys.TransactionStatus.name, transactionStatus)
                    })
                    finish()
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        }
        REQUEST_CODE_GOOGLE_PAY -> {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    handleGooglePaySuccess(data)
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    handleGooglePayFailure(data)
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        }
        else -> super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleGooglePaySuccess(intent: Intent?) {
        if (intent != null) {
            val paymentData = PaymentData.getFromIntent(intent)
            val token = paymentData?.paymentMethodToken?.token

            if (token != null) {
                gPayToken = token
                verifyV3(amount(), layoutId())
            }
        }
    }

    private fun handleGooglePayFailure(intent: Intent?) {
        val status = AutoResolveHelper.getStatusFromIntent(intent)
        Log.w("loadPaymentData failed", String.format("Payment error code: %s", status.toString()))
    }

    private fun isValid(): Boolean {

        val amount = amount()

        if (amount.isEmpty()) {

            errorMode(amount.isEmpty(), binding.editTextAmount)

            if (amount.isEmpty()) {
                binding.textViewAmountDesc.setTextColor(ContextCompat.getColor(this, R.color.error))
            } else {
                binding.textViewAmountDesc.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.not_error
                    )
                )
            }

            return false
        } else if (amount.toInt() < minAmount || amount.toInt() > maxAmount) {

            errorMode(amount.toInt() < minAmount || amount.toInt() > maxAmount, binding.editTextAmount)

            if (amount.toInt() < minAmount || amount.toInt() > maxAmount) {
                binding.textViewAmountDesc.setTextColor(ContextCompat.getColor(this, R.color.error))
            } else {
                binding.textViewAmountDesc.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.not_error
                    )
                )
            }

            return false
        }

        return true
    }

    override fun cryptogram(): String {
        return gPayToken
    }

    override fun layoutId(): String {
        return layoutId
    }

    override fun amount(): String {
        return binding.editTextAmount.text.toString()
    }

    override fun comment(): String {
        return binding.editTextComment.text.toString()
    }

    override fun photoUrl(): String {
        return photoUrl
    }

    override fun name(): String {
        return name
    }

    override fun feeFromPayer(): Boolean {
        return binding.feeSwitch.isChecked
    }
}