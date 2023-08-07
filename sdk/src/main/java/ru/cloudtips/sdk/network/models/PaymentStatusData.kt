package ru.cloudtips.sdk.network.models

data class PaymentStatusData(
    private val status: String?
) {
    fun getStatus(): Status {
        return Status.values().find { it.name.equals(status, true) } ?: Status.Failed
    }

    enum class Status {
        Wait, Success, Failed;
    }
}
