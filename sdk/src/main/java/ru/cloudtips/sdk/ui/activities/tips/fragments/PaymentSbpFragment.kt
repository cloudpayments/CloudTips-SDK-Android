package ru.cloudtips.sdk.ui.activities.tips.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.amplitude
import ru.cloudtips.sdk.databinding.FragmentPaymentSbpBottomBinding
import ru.cloudtips.sdk.helpers.AcknowledgeStatus
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.helpers.SbpHelper
import ru.cloudtips.sdk.helpers.observeOnce
import ru.cloudtips.sdk.network.models.PaymentExternalData
import ru.cloudtips.sdk.network.models.PaymentPageData
import ru.cloudtips.sdk.ui.activities.tips.adapters.SbpBankListAdapter
import ru.cloudtips.sdk.ui.activities.tips.listeners.IPaymentSbpListener
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.SbpAcknowledgeViewModel
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel

class PaymentSbpFragment : BottomSheetDialogFragment(), SbpBankListAdapter.ISbpBankListListener {

    private val viewBinding: FragmentPaymentSbpBottomBinding by viewBinding()
    private val viewModel: TipsViewModel by activityViewModels()
    private val acknowledgeViewModel: SbpAcknowledgeViewModel by viewModels()

    private var behavior: BottomSheetBehavior<*>? = null
    private var banksAdapter = SbpBankListAdapter(this)

    private var listener: IPaymentSbpListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? IPaymentSbpListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogThemeNoFloating)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_payment_sbp_bottom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        behavior = (dialog as BottomSheetDialog).behavior
//        behavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED
//        behavior?.isFitToContents = false


        viewModel.getPaymentPageData().observeOnce(viewLifecycleOwner) {
            initBanksList()
            updateViewsColor(it)
        }
    }

    private fun updateViewsColor(paymentPageData: PaymentPageData?) = with(viewBinding) {
        val buttonColor = paymentPageData?.getButtonsColor() ?: requireContext().getColor(R.color.colorAccent)
        CommonHelper.setViewTint(notInstalledButton, buttonColor)
        CommonHelper.setViewTint(bankNameLayout, buttonColor)
        showAllBanksButton.setTextColor(buttonColor)
    }

    private fun initBanksList() = with(viewBinding) {
        bankNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                fillBanksList(p0?.toString() ?: "")
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        banksRecyclerView.apply {
            adapter = banksAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
        fillBanksList()
        fillSavedBankLayout()
    }

    private fun fillBanksList(filter: String = "") = with(viewBinding) {
        hideBankNotInstalledWarning()
        val banks = SbpHelper.getBanksWithFilter(filter)
        if (banks.isNotEmpty()) {
            banksRecyclerView.visibility = View.VISIBLE
            emptyBanksView.visibility = View.GONE
            banksAdapter.setData(banks)
        } else {
            banksRecyclerView.visibility = View.GONE
            emptyBanksView.visibility = View.VISIBLE
        }
    }

    override fun onSbpBankClick(item: SbpHelper.SbpBank) {
        showBankIsOpening(item)
        amplitude.trackSbpBankClick(item)
        viewModel.launchSbpPayment().observe(viewLifecycleOwner) {
            hideBankIsOpening()
            if (it.succeed) {
                val data = it.data
                val link = SbpHelper.buildLink(item, data?.universalLinkUrl)
                if (CommonHelper.launchWebUrl(requireContext(), link)) {
                    launchInProgress(data, item)
                } else {
                    showBankNotInstalledWarning()
                }
            }
        }
    }

    private fun fillSavedBankLayout() = with(viewBinding) {
        val item = SbpHelper.getSavedBank() ?: return@with
        showAllBanksButton.setOnClickListener { showBanks() }
        with(savedBankItem) {
            root.setOnClickListener { onSbpBankClick(item) }
            labelView.text = item.name
            Glide.with(avatarView).load(item.icon).into(avatarView)
        }
        savedBankLayout.visibility = View.VISIBLE
        banksRecyclerView.visibility = View.GONE
    }

    private fun showBanks() = with(viewBinding) {
        banksRecyclerView.visibility = View.VISIBLE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.GONE
        inProgressView.visibility = View.GONE
        savedBankLayout.visibility = View.GONE
    }

    private fun launchInProgress(data: PaymentExternalData?, item: SbpHelper.SbpBank) {
        showInProgress()
        acknowledgeViewModel.startAcknowledge(data, item).observe(viewLifecycleOwner) { status ->
            when (status) {
                AcknowledgeStatus.SUCCESS -> {
                    listener?.onSbpPaymentSuccess()
                    dismissAllowingStateLoss()
                }
                AcknowledgeStatus.FAIL -> {
                    listener?.onSbpPaymentFailure()
                    dismissAllowingStateLoss()
                }
                AcknowledgeStatus.TIMEOUT -> {
                    showTimeoutWarning()
                }
            }
        }
    }

    private fun showTimeoutWarning() = with(viewBinding) {
        banksRecyclerView.visibility = View.GONE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.GONE
        inProgressView.visibility = View.GONE
        savedBankLayout.visibility = View.GONE
        bankOpeningLayout.visibility = View.GONE
        timeoutView.visibility = View.VISIBLE
        timeoutButton.setOnClickListener {
            hideBankNotInstalledWarning()
        }
    }

    private fun showBankIsOpening(bank: SbpHelper.SbpBank) = with(viewBinding) {
        banksRecyclerView.visibility = View.GONE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.GONE
        inProgressView.visibility = View.GONE
        savedBankLayout.visibility = View.GONE
        bankOpeningLayout.visibility = View.VISIBLE
        timeoutView.visibility = View.GONE
        bankOpeningName.text = getString(R.string.sbp_bank_is_opening_label, bank.name)
    }

    private fun hideBankIsOpening() = with(viewBinding) {
        bankOpeningLayout.visibility = View.GONE
    }

    private fun showBankNotInstalledWarning() = with(viewBinding) {
        banksRecyclerView.visibility = View.GONE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.VISIBLE
        inProgressView.visibility = View.GONE
        savedBankLayout.visibility = View.GONE
        timeoutView.visibility = View.GONE
        notInstalledButton.setOnClickListener {
            hideBankNotInstalledWarning()
        }
    }

    private fun hideBankNotInstalledWarning() = with(viewBinding) {
        banksRecyclerView.visibility = View.VISIBLE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.GONE
        inProgressView.visibility = View.GONE
        savedBankLayout.visibility = View.GONE
        timeoutView.visibility = View.GONE
    }

    private fun showInProgress() = with(viewBinding) {
        bankNameLayout.isEnabled = false
        banksRecyclerView.visibility = View.GONE
        emptyBanksView.visibility = View.GONE
        notInstalledBankView.visibility = View.GONE
        inProgressView.visibility = View.VISIBLE
        savedBankLayout.visibility = View.GONE
        timeoutView.visibility = View.GONE
    }

    companion object {
        fun newInstance() = PaymentSbpFragment()
    }

}