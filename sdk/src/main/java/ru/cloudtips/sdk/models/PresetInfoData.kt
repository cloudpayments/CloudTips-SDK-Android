package ru.cloudtips.sdk.models

import android.util.Range
import ru.cloudtips.sdk.helpers.CommonHelper
import kotlin.math.ceil

data class PresetInfoData(val type: Type, val values: List<Preset>) {

    private var selected: Int = -1

    fun getSelectedIndexBySum(sum: Double): Int {
        return when (type) {
            Type.Add -> return -1
            Type.Value,
            Type.Percent -> values.indexOfFirst { it.value == sum }
        }
    }

    enum class Type {
        Add, Value, Percent;
    }

    data class Preset(val value: Double, val title: String)

    companion object {
        fun buildByAdd(values: List<Double>): PresetInfoData {
            return PresetInfoData(Type.Add, values.map { Preset(it, "+${CommonHelper.formatDouble(it, " ₽")}") })
        }

        fun buildByValue(values: List<Double>, range: Pair<Double, Double>): PresetInfoData {
            return PresetInfoData(Type.Value, values.filter { it in range.first..range.second }
                .map { Preset(it, CommonHelper.formatDouble(it, " ₽")) })
        }

        fun buildByPercent(values: List<Double>, sum: Double, range: Pair<Double, Double>): PresetInfoData {
            return PresetInfoData(
                Type.Percent,
                values.filter { ceil(sum * it / 100.0) in range.first..range.second }
                    .map { Preset(ceil(sum * it / 100.0), CommonHelper.formatDouble(it, " %")) })
        }
    }
}