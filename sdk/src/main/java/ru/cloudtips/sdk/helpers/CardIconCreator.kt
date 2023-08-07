package ru.cloudtips.sdk.helpers

import android.text.TextUtils
import android.widget.ImageView
import ru.cloudtips.edit_card.editcard.util.CardImageLoader
import ru.cloudtips.edit_card.R
import ru.cloudtips.sdk.models.CardData

class CardIconCreator : CardImageLoader {

    private var hardType: Int? = null
    fun setHardType(type: CardData.Type?) {
        hardType = when (type) {
            CardData.Type.VISA -> VISA
            CardData.Type.MASTERCARD -> MASTER_CARD
            CardData.Type.MIR -> MIR
            CardData.Type.MAESTRO -> MAESTRO
            else -> null
        }
    }

    override fun loadImage(cardImageView: ImageView, numberPart: String): Boolean {
        val cardType = hardType ?: getCardType(numberPart)

        return loadSimpleImage(cardImageView, cardType)
    }

    private fun loadSimpleImage(cardImageView: ImageView, cardType: Int): Boolean {
        return when (cardType) {
            MASTER_CARD -> {
                cardImageView.setImageResource(R.drawable.core_ic_editcard_master)
                return true
            }
            VISA -> {
                cardImageView.setImageResource(R.drawable.core_ic_editcard_visa)
                return true
            }
            MIR -> {
                cardImageView.setImageResource(R.drawable.core_ic_editcard_mir)
                return true
            }
            MAESTRO -> {
                cardImageView.setImageResource(R.drawable.core_ic_editcard_maestro)
                return true
            }
            UNKNOWN -> false
            else -> false
        }
    }

    private fun getCardType(cardNumber: String): Int {
        if (!TextUtils.isEmpty(cardNumber)) {
            val number = cardNumber.replace(" ", "")
            if (number.startsWith("4")) return VISA
            if (number.length < 2) return UNKNOWN
            val twoDigits = number.substring(0, 2).toIntOrNull() ?: return UNKNOWN
            if (twoDigits in 51..55) return MASTER_CARD
            if (number.length < 4) return UNKNOWN
            val fourDigits = number.substring(0, 4).toIntOrNull() ?: return UNKNOWN
            if (fourDigits in 2221..2720) return MASTER_CARD
            if (fourDigits in 2200..2204) return MIR
            if (fourDigits in listOf(5018, 5020, 5038, 5893, 6304, 6759, 6761, 6762, 6763))
                return MAESTRO

            return UNKNOWN

        }
        return UNKNOWN
    }

    companion object {

        private val MASTER_CARD = 1
        private val VISA = 2
        private val MAESTRO = 3
        private val MIR = 4
        private val UNKNOWN = 5
    }
}