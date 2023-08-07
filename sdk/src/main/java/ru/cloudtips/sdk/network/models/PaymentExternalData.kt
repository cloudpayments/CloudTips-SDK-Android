package ru.cloudtips.sdk.network.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentExternalData(
    val identifier: String?,
    val universalLinkUrl: String?
) : Parcelable