package ru.cloudtips.sdk.network.postbodies

data class PostDeviceInfo(
    val type: Int = 1,
    val os: String = "Android",
    val webView: Boolean = false,
    val browser: String? = "androidsdk"
)