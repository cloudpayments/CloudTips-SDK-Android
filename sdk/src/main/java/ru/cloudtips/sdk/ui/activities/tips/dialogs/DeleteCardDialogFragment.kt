package ru.cloudtips.sdk.ui.activities.tips.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.ui.activities.tips.viewmodels.TipsViewModel

class DeleteCardDialogFragment : DialogFragment() {

    private val viewModel: TipsViewModel by activityViewModels()
    private var customView: View? = null

    private var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = layoutInflater.inflate(R.layout.fragment_delete_card_dialog, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(customView)

        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onDestroyView() {
        customView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView = view.findViewById<TextView>(R.id.title_textview)
        val messageTextView = view.findViewById<TextView>(R.id.message_textview)
        val cancelButton = view.findViewById<View>(R.id.cancel_button)
        val deleteButton = view.findViewById<View>(R.id.delete_button)

        viewModel.getSavedCards().observe(viewLifecycleOwner) { cards ->
            if (cards.size == 1) {
                titleTextView.setText(R.string.dialog_delete_card_title)
                messageTextView.setText(R.string.dialog_delete_card_description)
            } else {
                titleTextView.setText(R.string.dialog_delete_card_multi_title)
                messageTextView.setText(R.string.dialog_delete_card_multi_description)
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        deleteButton.setOnClickListener {
            listener?.onDeleteCard()
            dismiss()
        }

        viewModel.getPaymentPageData().observe(viewLifecycleOwner) {
            val buttonColor = it?.getButtonsColor() ?: requireContext().getColor(R.color.colorAccent)
            CommonHelper.setViewTint(cancelButton, buttonColor)
            CommonHelper.setViewTint(deleteButton, buttonColor)
        }

    }

    interface Listener {
        fun onDeleteCard()
    }

    companion object {
        fun newInstance(listener: Listener? = null) = DeleteCardDialogFragment().apply {
            this.listener = listener
        }
    }
}