package ru.cloudtips.sdk.ui.activities.tips.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import ru.cloudtips.sdk.R
import ru.cloudtips.sdk.databinding.RvItemSbpBankBinding
import ru.cloudtips.sdk.helpers.SbpHelper

class SbpBankListAdapter(private val listener: ISbpBankListListener?) : RecyclerView.Adapter<SbpBankListAdapter.ViewHolder>() {
    interface ISbpBankListListener {
        fun onSbpBankClick(item: SbpHelper.SbpBank)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewBinding: RvItemSbpBankBinding by viewBinding()
        fun bind(item: SbpHelper.SbpBank) {
            with(viewBinding) {
                Glide.with(avatarView).load(item.icon).into(avatarView)
                labelView.text = item.name
                itemView.setOnClickListener {
                    listener?.onSbpBankClick(items[bindingAdapterPosition])
                }
            }
        }
    }

    private val items: MutableList<SbpHelper.SbpBank> = mutableListOf()

    fun setData(list: List<SbpHelper.SbpBank>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_sbp_bank, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}