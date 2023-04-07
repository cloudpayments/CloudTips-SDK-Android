package ru.cloudtips.sdk.helpers

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import ru.cloudtips.sdk.R
import java.text.SimpleDateFormat
import java.util.*


object CommonHelper {
    @SuppressLint("SimpleDateFormat")
    fun stringToDate(date: String?): Date? {
        if (date == null) return null
        val formatList = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "dd MMMM yyyy"
        )
        for (format in formatList) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(date)
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    fun formatDouble(_value: Double?, prefix: String = ""): String {
        val value = _value ?: 0.0
        return when {
            value - value.toInt() < 1e-6 -> {
                "%.0f"
            }
            value * 10 - (value * 10).toInt() < 1e-6 -> {
                "%.1f"
            }
            else -> "%.2f"
        }.format(value.toFloat()).plus(prefix)
    }

    fun getColorByString(value: String?): Int? {
        return try {
            Color.parseColor(value)
        } catch (e: Exception) {
            null
        }
    }


    fun setViewTint(view: View, color: Int) {
        when (view) {
            is AppCompatButton -> {
                view.backgroundTintList = ColorStateList.valueOf(color)
            }
            is AppCompatImageButton -> {
                view.imageTintList = ColorStateList.valueOf(color)
            }
            is AppCompatImageView -> {
                view.imageTintList = ColorStateList.valueOf(color)
            }
            is SwitchCompat -> {
                setCheckboxColor(view, color)
            }
            is ProgressBar -> {
                view.progressTintList = ColorStateList.valueOf(color)
            }
            else -> {
                view.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    private fun setCheckboxColor(switch: SwitchCompat, colorInt: Int) {
        val states = arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked))

        val r = Color.red(colorInt)
        val g = Color.green(colorInt)
        val b = Color.blue(colorInt)
        val a = Color.alpha(colorInt) / 2

        val colorTrack = Color.argb(a, r, g, b)
        val colorThumb = colorInt

        val defaultTrack = ContextCompat.getColor(switch.context, R.color.colorSwitchInactive)
        val defaultThumb = ContextCompat.getColor(switch.context, R.color.colorWhite)

        val thumbColors = intArrayOf(
            defaultThumb,
            colorThumb
        )

        val trackColors = intArrayOf(
            defaultTrack,
            colorTrack
        )

        DrawableCompat.setTintList(DrawableCompat.wrap(switch.thumbDrawable), ColorStateList(states, thumbColors))
        DrawableCompat.setTintList(DrawableCompat.wrap(switch.trackDrawable), ColorStateList(states, trackColors))
    }

}