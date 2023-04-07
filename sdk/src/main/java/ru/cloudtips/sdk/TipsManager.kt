package ru.cloudtips.sdk

import android.content.Context
import ru.cloudtips.sdk.helpers.AmplitudeManager
import ru.cloudtips.sdk.network.NetworkClient
import ru.cloudtips.sdk.ui.activities.tips.PaymentTipsActivity

internal val networkClient: NetworkClient by lazy {
    TipsManager.mNetworkClient!!
}

val amplitude: AmplitudeManager by lazy {
    TipsManager.mAmplitude!!
}

class TipsManager(private val context: Context) {
    init {
        mNetworkClient = NetworkClient()
        mAmplitude = AmplitudeManager(context)
    }

    fun launch(layoutId: String?, sum: Double? = null) {
        context.startActivity(PaymentTipsActivity.newIntent(context, layoutId, sum))
    }


    companion object {
        internal var mNetworkClient: NetworkClient? = null
        internal var mAmplitude: AmplitudeManager? = null

        fun getInstance(context: Context): TipsManager {
            return TipsManager(context)
        }
    }
}