package ru.cloudtips.sdk.ui.activities.tips.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import by.kirich1409.viewbindingdelegate.viewBinding
import ru.cloudtips.edit_card.editcard.util.CardImageLoader
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.databinding.RvItemPaymentCardSingleBinding
import ru.cloudtips.sdk.helpers.CardIconCreator
import ru.cloudtips.sdk.helpers.CommonHelper
import ru.cloudtips.sdk.models.CardData

class CardListAdapter(private val listener: Listener) :
    Adapter<CardListAdapter.ViewHolder>() {

    private var data: List<CardData> = emptyList()
    private var accentColor: Int? = null

    interface Listener {
        fun onCardDataEntered(item: CardData, cvc: String)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewBinding: RvItemPaymentCardSingleBinding by viewBinding()
        private var filled: Boolean = false
        fun bind(item: CardData) = with(viewBinding) {
            if (!filled) {
                filled = true
                editCard.apply {
                    hideDate()
                    setBlockedMaskedCardNumber(item.getNumber() ?: "")
                    setImageLoader(CardIconCreator().apply {
                        setHardType(item.getType())
                    })
                    cvc = null
                    dateEditText.isAllowExpired = true
                    addCardTextChangedListener { field, id, formatted, unformatted ->
                        if (field == cvcEditText) {
                            onItemSelected()
                        }
                    }
                    cvcEditText.setOnFocusChangeListener { view, hasFocus ->
                        if (hasFocus) onItemSelected()
                        bottomLineView.isSelected = hasFocus
                    }
                    setShortMode()
                    CommonHelper.setLineSelectorColor(bottomLineView, accentColor)

                    fakeClickView.setOnClickListener {
                        cvcEditText.requestFocus()
                        CommonHelper.showKeyboard(cvcEditText)
                    }
                }
            }
        }

        private fun onItemSelected() = with(viewBinding) {
            listener.onCardDataEntered(data[bindingAdapterPosition], editCard.cvc)
        }
    }

    fun setData(newData: List<CardData>) {
        data = newData
        notifyDataSetChanged()
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_payment_card_single, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount() = data.size

}