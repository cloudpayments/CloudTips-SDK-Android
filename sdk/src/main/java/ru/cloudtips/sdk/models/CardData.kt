package ru.cloudtips.sdk.models

class CardData(
    private var number: String?,
    private val externalId: String?,
    private val cardType: String?
) {
    fun getExternalId() = externalId
    fun getNumber() = number

    fun getType(): Type? {
        return Type.values().find { it.name.equals(cardType, true) }
    }

    enum class Type {
        VISA, MASTERCARD, MIR, MAESTRO;
    }
}