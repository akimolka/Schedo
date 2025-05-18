package org.schedo.repository

import kotlinx.serialization.Serializable
import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime

enum class Status {
    SCHEDULED, ENQUEUED, STARTED, COMPLETED, FAILED
}

@Serializable
data class AdditionalInfo(
    val errorMessage: String? = null,
    val stackTrace: List<String>? = null,
) {
    fun merge(rhs: AdditionalInfo): AdditionalInfo {
        val lhs = this
        return AdditionalInfo(
            errorMessage = lhs.errorMessage ?: rhs.errorMessage,
            stackTrace = lhs.stackTrace ?: rhs.stackTrace,
        )
    }
}

interface StatusRepository {
    fun insert(instance: TaskInstanceID, moment: OffsetDateTime)
    fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo = AdditionalInfo())
}