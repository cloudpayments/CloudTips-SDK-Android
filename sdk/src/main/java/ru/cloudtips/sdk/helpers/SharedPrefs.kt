package ru.cloudtips.sdk.helpers

import android.content.Context
import android.content.SharedPreferences

class SharedPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("ru.cloudtips.sdk", Context.MODE_PRIVATE)

    private inline fun <reified T> SharedPreferences.get(key: String, defaultValue: T?): T? {
        when (T::class) {
            Boolean::class -> return this.getBoolean(key, defaultValue as Boolean) as? T
            Float::class -> return this.getFloat(key, defaultValue as Float) as? T
            Int::class -> return this.getInt(key, defaultValue as Int) as? T
            Long::class -> return this.getLong(key, defaultValue as Long) as? T
            String::class -> return this.getString(key, defaultValue as? String) as? T
            else -> {
                if (defaultValue is Set<*>) {
                    return this.getStringSet(key, defaultValue as Set<String>) as? T
                }
            }
        }

        return defaultValue
    }

    private inline fun <reified T> SharedPreferences.put(key: String, value: T?) {
        val editor = this.edit()

        when (T::class) {
            Boolean::class -> editor.putBoolean(key, value as Boolean)
            Float::class -> editor.putFloat(key, value as Float)
            Int::class -> editor.putInt(key, value as Int)
            Long::class -> editor.putLong(key, value as Long)
            String::class -> editor.putString(key, value as? String)
            else -> {
                if (value is Set<*>) {
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }

        editor.commit()
    }

    private val CARD_EXTERNAL_ID = "CARD_EXTERNAL_ID"
    var cardExternalId: String?
        get() = prefs.get(CARD_EXTERNAL_ID, null)
        set(value) = prefs.put(CARD_EXTERNAL_ID, value)

    private val SBP_SAVED_BANK_SCHEMA = "SBP_SAVED_BANK_SCHEMA"
    var sbpSavedBankSchema: String?
        get() = prefs.get(SBP_SAVED_BANK_SCHEMA, null)
        set(value) = prefs.put(SBP_SAVED_BANK_SCHEMA, value)
}