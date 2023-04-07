package ru.cloudtips.sdk.ui.elements

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class ClickableUrlSpan(
    private val url: String,
    private val listener: ISpanUrlClick?,
    private var isUnderline: Boolean = false,
    private val color: Int? = null
) : ClickableSpan() {

    override fun onClick(view: View) {
        listener?.onUrlClick(url)
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = isUnderline
        if (color != null) ds.color = color
    }

    interface ISpanUrlClick {
        fun onUrlClick(url: String?)
    }
}