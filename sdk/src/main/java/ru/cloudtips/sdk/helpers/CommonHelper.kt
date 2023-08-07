package ru.cloudtips.sdk.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.util.Log
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ru.cloudtips.sdk.BuildConfig
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
            is TextInputLayout -> {
                view.setEndIconTintList(ColorStateList.valueOf(color))
            }
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
                setSwitchColor(view, color)
            }
            is CheckBox -> {
                setCheckboxColor(view, color)
            }
            is ProgressBar -> {
                view.progressTintList = ColorStateList.valueOf(color)
            }
            is TextView -> {
                view.setTextColor(color)
                view.compoundDrawablesRelative.forEach { it?.setTint(color) }
            }
            else -> {
                view.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    fun setLineSelectorColor(view: View, colorInt: Int?) {
        if (colorInt == null) return
        val states = arrayOf(intArrayOf(-android.R.attr.state_selected), intArrayOf(android.R.attr.state_selected))

        val defaultColor = ContextCompat.getColor(view.context, R.color.colorBottomLineInactive)

        val colors = intArrayOf(defaultColor, colorInt)

        DrawableCompat.setTintList(DrawableCompat.wrap(view.background), ColorStateList(states, colors))
    }

    private fun setSwitchColor(switch: SwitchCompat, colorInt: Int) {
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

    private fun setCheckboxColor(checkBox: CheckBox, colorInt: Int) {
        val states = arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked))

        val defaultColor = ContextCompat.getColor(checkBox.context, R.color.colorSwitchInactive)

        val colors = intArrayOf(
            defaultColor,
            colorInt
        )

        val drawable = checkBox.buttonDrawable ?: return
        DrawableCompat.setTintList(DrawableCompat.wrap(drawable), ColorStateList(states, colors))
    }

    fun hideKeyboard(view: View?) {
        if (view == null) return
        val inputMethodManager = view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: View?) {
        if (view == null) return
        val inputMethodManager = view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun requestCaptcha(activity: Activity, callback: (String) -> Unit) {
        SafetyNet.getClient(activity).verifyWithRecaptcha(BuildConfig.RECAPTCHA_TOKEN)
            .addOnSuccessListener(activity) { result ->
                // Indicates communication with reCAPTCHA service was
                // successful.
                val token = result.tokenResult
                if (!token.isNullOrEmpty()) {
                    callback(token)
                }
            }
            .addOnFailureListener(activity) { e ->
                if (e is ApiException) {
                    // An error occurred when communicating with the
                    // reCAPTCHA service. Refer to the status code to
                    // handle the error appropriately.
                    Log.d("recaptcha", "Error: ${CommonStatusCodes.getStatusCodeString(e.statusCode)}")
                } else {
                    // A different, unknown type of error occurred.
                    Log.d("recaptcha", "Error: ${e.message}")
                }
            }
    }

    fun launchWebUrl(context: Context, url: String?): Boolean {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (browserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(browserIntent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun isTinkoffBankAvailable(context: Context): Boolean {
        val targetPackage = "com.idamob.tinkoff.android"
        return try {
            context.packageManager.getPackageInfo(targetPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getIdFromMetadata(context: Context): String? {
        return try {
            val app = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = app.metaData
            bundle.getString(context.getString(R.string.cloudtips_metadata_link_id))
        } catch (e: Exception) {
            null
        }
    }

}