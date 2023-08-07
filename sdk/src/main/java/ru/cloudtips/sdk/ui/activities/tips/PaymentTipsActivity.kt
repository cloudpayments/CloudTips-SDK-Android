package ru.cloudtips.sdk.ui.activities.tips

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.helpers.*
import ru.cloudtips.sdk.network.models.PaymentExternalData
import ru.cloudtips.sdk.networkClient
import ru.cloudtips.sdk.ui.activities.tips.fragments.*
import ru.cloudtips.sdk.ui.activities.tips.listeners.*
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel

class PaymentTipsActivity : AppCompatActivity(), IPaymentInfoListener, IPaymentCardListener, IPaymentSuccessListener, IPaymentTinkoffListener,
    IPaymentSbpListener {

    private val viewModel: TipsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_payment_tips)
        if (savedInstanceState == null) startPayment()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    private fun buildSbpPayDeeplink(): String? {
        val tinkoffPayTipsId = CommonHelper.getIdFromMetadata(this) ?: return null
        return Uri.Builder().scheme(getString(R.string.sbp_deeplink_scheme)).path(tinkoffPayTipsId).build().toString().replace("/", "//")
    }

    private fun startPayment() {
        val layoutId = intent.getStringExtra(EXTRA_LAYOUT_ID)
        val sum = intent.getDoubleExtra(EXTRA_SUM, 0.0)
        amplitude.setLayoutId(layoutId)
        viewModel.setSbpPayDeeplink(buildSbpPayDeeplink())
        viewModel.setTinkoffPayDeeplink(buildTinkoffPayDeeplink())
        viewModel.requestPaymentPageData(layoutId, sum).observe(this) { response ->
            if (!response.succeed || response.data == null) {
                onShowError()
            }
        }
        replaceFragment(PaymentInfoFragment.newInstance(), R.id.container)
    }

    private fun buildTinkoffPayDeeplink(): String? {
        val tinkoffPayTipsId = CommonHelper.getIdFromMetadata(this) ?: return null
        return Uri.Builder().scheme(getString(R.string.tpay_deeplink_scheme)).path(tinkoffPayTipsId).build().toString().replace("/", "//")
    }

    private fun onShowError() {
        replaceFragment(PaymentErrorFragment.newInstance(), R.id.container)
    }

    override fun onPayInfoTinkoffLaunch(data: PaymentExternalData?) {
        if (data == null) onPaymentFailure(PayType.TINKOFFPAY)
        else addFragmentToBackStack(PaymentTinkoffAcknowledgeFragment.newInstance(data), R.id.container)
    }

    override fun onTinkoffPaymentSuccess() {
        cleanBackStack()
        onPaymentSuccess(PayType.TINKOFFPAY)
    }

    override fun onTinkoffPaymentFailure() {
        cleanBackStack()
        onPaymentFailure(PayType.TINKOFFPAY)
    }

    override fun onSbpPaymentSuccess() {
        cleanBackStack()
        onPaymentSuccess(PayType.SBP)
    }

    override fun onSbpPaymentFailure() {
        cleanBackStack()
        onPaymentFailure(PayType.SBP)
    }

    override fun onPaymentSuccess(payType: PayType) {
        replaceFragment(PaymentSuccessFragment.newInstance(payType), R.id.container)
    }

    override fun onPaymentFailure(payType: PayType) {
        replaceFragmentWithBackStack(PaymentFailureFragment.newInstance(payType), R.id.container)
    }

    override fun onPayInfoSbpLaunch() {
        showBottomDialog(PaymentSbpFragment.newInstance())
    }

    override fun onSuccessClick() {
        cleanBackStack()
        startPayment()
    }

    override fun onPaymentCardSuccess() {
        networkClient.clearXRequestId()
        onPaymentSuccess(PayType.CARD)
    }

    override fun onPaymentCardFailure() {
        networkClient.clearXRequestId()
        onPaymentFailure(PayType.CARD)
    }

    override fun onCloseClick() {
        finish()
    }

    companion object {
        private const val EXTRA_LAYOUT_ID = "EXTRA_LAYOUT_ID"
        private const val EXTRA_SUM = "EXTRA_SUM"
        fun newIntent(context: Context, layoutId: String?, sum: Double? = null) =
            Intent(context, PaymentTipsActivity::class.java).apply {
                putExtra(EXTRA_LAYOUT_ID, layoutId)
                putExtra(EXTRA_SUM, sum)
            }
    }

}