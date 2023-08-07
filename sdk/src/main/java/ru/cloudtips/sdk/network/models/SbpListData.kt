package ru.cloudtips.sdk.network.models

data class SbpListData(
    val dictionary: List<SbpBankData>?,
    val version: String?
) {
    data class SbpBankData(
        val bankName: String?,
        val logoURL: String?,
        val package_name: String?,
        val schema: String?
    )
}