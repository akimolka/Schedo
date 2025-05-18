package org.schedo.repository

import kotlinx.serialization.Serializable
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime

enum class Status {
    SCHEDULED, ENQUEUED, STARTED, COMPLETED, FAILED
}

@Serializable
data class AdditionalInfo(
    val errorMessage: String? = null,
    val stackTrace: List<String>? = null,
) {
    fun merge(rhs: AdditionalInfo?): AdditionalInfo {
        if (rhs == null) {
            return this
        } else {
            val lhs = this
            return AdditionalInfo(
                errorMessage = lhs.errorMessage ?: rhs.errorMessage,
                stackTrace = lhs.stackTrace ?: rhs.stackTrace,
            )
        }
    }
}

class FinishedTask(
    val instanceID: TaskInstanceID,
    val taskName: TaskName,
    val status: Status,
    val finishedAt: OffsetDateTime,
    val additionalInfo: AdditionalInfo,
)


interface StatusRepository {
    fun insert(instance: TaskInstanceID, moment: OffsetDateTime)
    fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime,
                     info: AdditionalInfo? = null)
    fun finishedTasks(): List<FinishedTask>
}