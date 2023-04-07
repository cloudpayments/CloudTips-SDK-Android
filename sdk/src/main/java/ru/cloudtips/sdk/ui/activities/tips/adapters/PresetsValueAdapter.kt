package ru.cloudtips.sdk.ui.activities.tips.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.helpers.CommonHelper

class PresetsValueAdapter(private val items: List<Int>, private val colorAccent: Int, private val listener: Listener) :
    RecyclerView.Adapter<PresetsValueAdapter.ViewHolder>() {

    interface Listener {
        fun onPresetValueClicked(item: Int)
    }

    private var checkedValue: Int = -1

    fun setCheckedValue(value: Int) {
        val prevCheckedValue = checkedValue
        checkedValue = value

        val prevIndex = items.indexOf(prevCheckedValue)
        if (prevIndex > 0) notifyItemChanged(prevIndex)
        val nextIndex = items.indexOf(checkedValue)
        if (nextIndex > 0) notifyItemChanged(nextIndex)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<TextView>(R.id.text_view)
        fun bind(position: Int) {
            val item = items[position]
            textView.text = item.toString()
            updateChecked(item == checkedValue)
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onPresetValueClicked(items[bindingAdapterPosition])
                }
            }
        }

        private fun updateChecked(isChecked: Boolean) {
            val backgroundColor: Int
            val fontColor: Int
            if (isChecked) {
                backgroundColor = colorAccent
                fontColor = itemView.context.getColor(R.color.colorWhite)
            } else {
                backgroundColor = itemView.context.getColor(R.color.colorBubbleBackground)
                fontColor = colorAccent
            }

            CommonHelper.setViewTint(itemView, backgroundColor)
            textView.setTextColor(fontColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_payment_bubble, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size
}