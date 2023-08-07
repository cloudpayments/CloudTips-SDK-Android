package ru.cloudtips.sdk.helpers

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.cloudtips.sdk.network.ResultWrapper
import ru.cloudtips.sdk.networkClient
import ru.cloudtips.sdk.sharedPrefs

object SbpHelper {

    data class SbpBank(val name: String?, val schema: String?, val icon: String?)

    private val banks = mutableListOf<SbpBank>()

    fun init() {
        CoroutineScope(Dispatchers.IO).launch {
            banks.clear()
            val response = networkClient.getSbpBankList()
            if (response is ResultWrapper.Success) {
                banks.addAll(response.value.dictionary?.map { SbpBank(it.bankName, it.schema, it.logoURL) } ?: emptyList())
            }
            banks.sortByDescending { it.name?.contains("Тинькофф", true) }
        }
    }

    fun getBanks(): List<SbpBank> {
        return banks
    }

    fun getBanksWithFilter(filter: String): List<SbpBank> {
        return banks.filter { it.name != null && it.name.contains(filter, true) }
    }

    fun buildLink(bank: SbpBank, url: String?): String? {
        if (url == null) return null
        val uri = Uri.parse(url)
        return Uri.Builder()
            .scheme(bank.schema)
            .encodedAuthority(uri.encodedAuthority)
            .path(uri.encodedPath)
            .encodedQuery(uri.encodedQuery)
            .build()
            .toString()
    }

    fun getSavedBank(): SbpBank? {
        val saved = sharedPrefs.sbpSavedBankSchema
        return banks.find { it.schema == saved }
    }

}