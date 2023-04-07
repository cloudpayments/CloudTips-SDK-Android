package ru.cloudtips.sdk.ui.activities.tips.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.databinding.FragmentPaymentSuccessBinding
import ru.cloudtips.sdk.helpers.PayType
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.network.models.PaymentPageData
import ru.cloudtips.sdk.ui.activities.tips.listeners.IPaymentSuccessListener
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel
import kotlin.math.max
import kotlin.math.roundToInt

class PaymentSuccessFragment : Fragment(R.layout.fragment_payment_success) {

    private val viewBinding: FragmentPaymentSuccessBinding by viewBinding()
    private val viewModel: TipsViewModel by activityViewModels()

    private var listener: IPaymentSuccessListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? IPaymentSuccessListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val payType = arguments?.getParcelable(EXTRA_PAY_TYPE) as? PayType
        amplitude.trackSuccessOpen(payType ?: PayType.CARD)

        viewModel.getPaymentPageData().observe(viewLifecycleOwner) {
            fillData(it)
            fillGoal(it?.getTarget())
        }

        with(viewBinding) {
            headerCloseButton.setOnClickListener {
                viewModel.trackPageClosed(payType)
                listener?.onCloseClick()
            }
            mainButton.setOnClickListener {
                listener?.onSuccessClick()
            }
        }

        viewModel.getPaymentPageData().observe(viewLifecycleOwner) {
            updateViewsColor(it)
        }
    }

    private fun updateViewsColor(paymentPageData: PaymentPageData?) = with(viewBinding) {
        val logo = paymentPageData?.getLogo()
        Glide.with(logoView).load(logo).error(R.drawable.ic_logo_horizontal).fitCenter().into(logoView)

        val buttonColor = paymentPageData?.getButtonsColor() ?: requireContext().getColor(R.color.colorAccent)
        CommonHelper.setViewTint(headerCloseButton, buttonColor)
        CommonHelper.setViewTint(mainButton, buttonColor)
        CommonHelper.setViewTint(goalProgressbarView, buttonColor)
    }

    private fun fillData(data: PaymentPageData?) = with(viewBinding) {
        if (data == null) return@with
        fillBackground(data.getBackground())
        if (!data.getUserName().isNullOrEmpty()) {
            titleTextview.visibility = View.VISIBLE
            titleTextview.text = data.getUserName()
        } else titleTextview.visibility = View.GONE
        val successMessage = data.getSuccessMessage()
        if (!successMessage.isNullOrEmpty()) subtitleTextview.text = successMessage
        Glide.with(avatarView).load(data.getUserAvatar())
            .placeholder(R.drawable.ic_empty_avatar)
            .error(R.drawable.ic_empty_avatar)
            .transform(CenterCrop(), RoundedCornersTransformation(resources.getDimensionPixelSize(R.dimen.profile_avatar_big_radius), 0))
            .into(avatarView)
    }

    private fun fillGoal(target: PaymentPageData.Target?) = with(viewBinding) {
        if (target == null) {
            priceGoalLayout.visibility = View.GONE
            return@with
        }
        priceGoalLayout.visibility = View.VISIBLE
        val maxValue = (target.getAmount() ?: 0.0).roundToInt()
        val curValue = (target.getCurrentAmount() ?: 0.0).roundToInt()
        val leftValue = max(maxValue - curValue, 0)
        goalTargetView.text = getString(R.string.main_balance, maxValue)
        goalCurrentView.text = getString(R.string.main_balance, leftValue)
        val progress = if (maxValue > 0) (100 * curValue / maxValue) else 0
        goalProgressbarView.progress = progress
    }


    private fun fillBackground(background: String?) = with(viewBinding) {
        if (background.isNullOrEmpty()) {
            backgroundImageLayout.visibility = View.GONE
        } else {
            backgroundImageLayout.visibility = View.VISIBLE
            Glide.with(backgroundImageView).load(background).centerCrop().into(backgroundImageView)
        }
    }

    companion object {
        private const val EXTRA_PAY_TYPE = "EXTRA_PAY_TYPE"
        fun newInstance(payType: PayType) = PaymentSuccessFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_PAY_TYPE, payType)
            }
        }
    }
}