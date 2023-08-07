package ru.cloudtips.sdk.network.models

data class SavedCardData(
    val cardId: String?,
    val cardMask: String?,
    val cardType: String?,
    val firstSix: String?,
    val lastFour: String?
)