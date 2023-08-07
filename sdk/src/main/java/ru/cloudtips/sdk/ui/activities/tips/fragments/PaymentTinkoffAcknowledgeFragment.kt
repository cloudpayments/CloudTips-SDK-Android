package ru.cloudtips.sdk.ui.activities.tips.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.databinding.FragmentPaymentTinkoffAcknowledgeBinding
import ru.cloudtips.sdk.helpers.AcknowledgeStatus
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.network.models.PaymentExternalData
import ru.cloudtips.sdk.ui.activities.tips.listeners.IPaymentTinkoffListener
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TinkoffAcknowledgeViewModel

class PaymentTinkoffAcknowledgeFragment : Fragment(R.layout.fragment_payment_tinkoff_acknowledge) {

    private val viewModel: TinkoffAcknowledgeViewModel by viewModels()
    private val viewBinding: FragmentPaymentTinkoffAcknowledgeBinding by viewBinding()

    private val data: PaymentExternalData by lazy { arguments?.getParcelable(EXTRA_DATA)!! }
    private var isLaunched = false

    private var listener: IPaymentTinkoffListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? IPaymentTinkoffListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onResume() {
        super.onResume()
        start()
    }

    private fun start() {
        if (!isLaunched) {
            isLaunched = true
            val url = data.universalLinkUrl
            if (url != null) {
                CommonHelper.launchWebUrl(requireContext(), url)
            } else {
                listener?.onTinkoffPaymentFailure()
            }
        } else {
            viewModel.startAcknowledge(data).observe(viewLifecycleOwner) { status ->
                when (status) {
                    AcknowledgeStatus.SUCCESS -> listener?.onTinkoffPaymentSuccess()
                    AcknowledgeStatus.FAIL -> listener?.onTinkoffPaymentFailure()
                    AcknowledgeStatus.TIMEOUT -> listener?.onTinkoffPaymentFailure()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewBinding) {
            closeButton.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    companion object {
        private const val EXTRA_DATA = "EXTRA_DATA"
        fun newInstance(data: PaymentExternalData) = PaymentTinkoffAcknowledgeFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_DATA, data)
            }
        }
    }
}