package ru.cloudtips.sdk.ui.activities.tips.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.card.ThreeDsDialogFragment
import ru.cloudtips.sdk.databinding.FragmentPaymentCardBottomBinding
import ru.cloudtips.sdk.helpers.CardIconCreator
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.helpers.PayType
import ru.cloudtips.sdk.models.CardData
import ru.cloudtips.sdk.models.PaymentInfoData
import ru.cloudtips.sdk.network.BasicResponse
import ru.cloudtips.sdk.network.hasNeedCaptcha
import ru.cloudtips.sdk.network.models.PaymentAuthData
import ru.cloudtips.sdk.network.models.PaymentAuthStatusCode
import ru.cloudtips.sdk.network.models.PaymentPageData
import ru.cloudtips.sdk.ui.activities.tips.adapters.CardListAdapter
import ru.cloudtips.sdk.ui.activities.tips.dialogs.DeleteCardDialogFragment
import ru.cloudtips.sdk.ui.activities.tips.listeners.IPaymentCardListener
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel

class PaymentCardBottomFragment : BottomSheetDialogFragment(R.layout.fragment_payment_card_bottom), CardListAdapter.Listener,
    ThreeDsDialogFragment.ThreeDSDialogListener {
    private val viewBinding: FragmentPaymentCardBottomBinding by viewBinding()
    private val viewModel: TipsViewModel by activityViewModels()

    private val cardsAdapter = CardListAdapter(this)

    private var listener: IPaymentCardListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? IPaymentCardListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCards()
        initAddCardLayout()

        viewModel.getPaymentPageData().observe(viewLifecycleOwner) {
            updateViewsColor(it)
        }

        viewModel.getPaymentInfoData().observe(viewLifecycleOwner) {
            hideSpinner()
            fillPrice(it)
        }
    }

    private fun fillPrice(paymentInfoData: PaymentInfoData) = with(viewBinding) {
        val text = getString(R.string.edit_card_pay_button_text, CommonHelper.formatDouble(paymentInfoData.getAmountWithFee()))
        payButton.text = text
        payDisabledButton.text = text
    }

    private fun updateViewsColor(paymentPageData: PaymentPageData?) = with(viewBinding) {
        val buttonColor = paymentPageData?.getButtonsColor() ?: requireContext().getColor(R.color.colorAccent)
        CommonHelper.setViewTint(payButton, buttonColor)
        CommonHelper.setViewTint(addCardButton, buttonColor)
        CommonHelper.setLineSelectorColor(newCardEditLayout.bottomLineView, buttonColor)
        cardsAdapter.setAccentColor(buttonColor)
    }

    private fun initCards() = with(viewBinding) {
        cardsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            cardsRecyclerView.adapter = cardsAdapter
        }
        viewModel.getSavedCards().observe(viewLifecycleOwner) { cards ->
            cardsAdapter.setData(cards)
            updateManageCardLayout(cards.isEmpty())
            updateDeleteCardButton(cards)
            if (cards.isEmpty()) addNewCard()
        }
    }

    override fun onCardDataEntered(item: CardData, cvc: String) {
        updateManageCardLayout(false)
        val isValid = cvc.length == 3
        setPayButtonEnabled(isValid)
        viewBinding.payButton.setOnClickListener {
            viewModel.putSavedPaymentCardData(item, cvc)
            launchPayment()
            viewModel.trackCardPayClick()
        }
    }

    private fun setPayButtonEnabled(isEnabled: Boolean) = with(viewBinding) {
        if (isEnabled) {
            payButton.visibility = View.VISIBLE
            payDisabledButton.visibility = View.GONE
        } else {
            payButton.visibility = View.GONE
            payDisabledButton.visibility = View.VISIBLE
        }
    }

    private fun initAddCardLayout() = with(viewBinding) {
        with(newCardEditLayout.editCard) {
            dateEditText.isAllowExpired = true
            val onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
                newCardEditLayout.bottomLineView.isSelected = hasFocus
                onNewCardDataEntered()
                updateManageCardLayout(true)
            }
            cardNumberEditText.onFocusChangeListener = onFocusChangeListener
            dateEditText.onFocusChangeListener = onFocusChangeListener
            cvcEditText.onFocusChangeListener = onFocusChangeListener
            cvcEditText.addTextChangedListener { field, id, formatted, unformatted ->
                onNewCardDataEntered()
            }

            setImageLoader(CardIconCreator())
            newCardEditLayout.fakeClickView.visibility = View.GONE
        }
        addCardButton.setOnClickListener {
            addNewCard()
        }
        deleteCardButton.setOnClickListener {
            deleteCard()
        }
    }

    private fun onNewCardDataEntered() = with(viewBinding) {
        val isValid = newCardEditLayout.editCard.cvc.length == 3
        setPayButtonEnabled(isValid)
        payButton.setOnClickListener {
            with(newCardEditLayout.editCard) {
                val cardNumber = number
                val cardDate = date
                val cardCvc = cvc
                val cardSaved = saveCardCheckbox.isChecked
                viewModel.putPaymentCardData(cardNumber, cardDate, cardCvc, cardSaved)
                launchPayment()
                viewModel.trackCardPayClick()
            }

        }
    }

    private fun updateDeleteCardButton(cards: List<CardData>) = with(viewBinding) {
        if (cards.isEmpty()) {
            deleteCardButton.visibility = View.GONE
        } else if (cards.size == 1) {
            deleteCardButton.visibility = View.VISIBLE
            deleteCardButton.setText(R.string.delete_card_label)
        } else {
            deleteCardButton.visibility = View.VISIBLE
            deleteCardButton.setText(R.string.delete_card_multi_label)
        }
    }

    private fun updateManageCardLayout(addNew: Boolean) = with(viewBinding) {
        if (addNew) {
            manageCardLayout.visibility = View.GONE
            newCardManageLayout.visibility = View.VISIBLE
        } else {
            manageCardLayout.visibility = View.VISIBLE
            newCardManageLayout.visibility = View.GONE
        }
        updateAddCardButton()
    }

    private fun addNewCard() = with(viewBinding) {
        if (!newCardLayout.isVisible) {
            newCardLayout.visibility = View.VISIBLE
            newCardEditLayout.editCard.requestFocus()
        }
        updateAddCardButton()
    }

    private fun updateAddCardButton() = with(viewBinding) {
        val enabled = !newCardLayout.isVisible
        addCardButton.visibility = if (enabled) View.VISIBLE else View.GONE
        addCardBlockButton.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun deleteCard() {
        DeleteCardDialogFragment.newInstance(object : DeleteCardDialogFragment.Listener {
            override fun onDeleteCard() {
                viewModel.deleteSavedCards()
            }
        }).show(childFragmentManager, "DIALOG_DELETE_CARD")
    }

    private fun launchPayment(captcha: String? = null) {
        showSpinner()
        viewModel.launchPayment(captcha).observe(viewLifecycleOwner) {
            hideSpinner()
            onPaymentResponse(it)
        }
    }

    private fun onPaymentResponse(response: BasicResponse<PaymentAuthData>) {
        if (response.succeed) {
            val data = response.data
            when (data?.getStatusCode()) {
                PaymentAuthStatusCode.NEED3DS -> {
                    val fragment3ds = ThreeDsDialogFragment.newInstance(data, PayType.CARD)
                    fragment3ds.show(parentFragmentManager, "NEED3DS")
                    fragment3ds.setTargetFragment(this@PaymentCardBottomFragment, REQUEST_CODE_3DS)
                }
                PaymentAuthStatusCode.SUCCESS -> {
                    onPaymentSuccess()
                }
                else -> {
                    onPaymentFailure()
                }
            }
        } else {
            if (response.getErrors().hasNeedCaptcha()) {
                CommonHelper.requestCaptcha(requireActivity()) {
                    launchPayment(it)
                }
            } else {
                onPaymentFailure()
            }
        }
    }

    override fun onAuthorizationCompleted(md: String, paRes: String, payType: PayType) {
        showSpinner()
        viewModel.postPayment3ds(md, paRes).observe(this) {
            hideSpinner()
            onPaymentResponse(it)
        }
    }

    override fun onAuthorizationFailed(error: String?, payType: PayType) {
        onPaymentFailure()
    }

    private fun onPaymentSuccess() {
        listener?.onPaymentCardSuccess()
        dismiss()
    }

    private fun onPaymentFailure() {
        listener?.onPaymentCardFailure()
        dismiss()
    }


    private fun showSpinner() = with(viewBinding) {
        spinnerLayout.root.visibility = View.VISIBLE
    }

    private fun hideSpinner() = with(viewBinding) {
        spinnerLayout.root.visibility = View.GONE
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        view?.let { CommonHelper.hideKeyboard(it) }
    }

    companion object {
        private const val REQUEST_CODE_3DS = 1001
        fun newInstance() = PaymentCardBottomFragment()
    }

}