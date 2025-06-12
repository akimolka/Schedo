package org.schedo.repository

import kotlinx.serialization.Serializable
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import org.schedo.util.KOffsetDateTimeSerializer
import java.time.OffsetDateTime

enum class Status {
    SCHEDULED, ENQUEUED, STARTED, COMPLETED, FAILED, CANCELLED
}

@Serializable
data class AdditionalInfo(
    val errorMessage: String? = null,
    val stackTrace: String? = null,
)

fun mergeInfo(lhs: AdditionalInfo?, rhs: AdditionalInfo?): AdditionalInfo? {
    if (lhs == null && rhs == null) return null

    return AdditionalInfo(
        errorMessage = lhs?.errorMessage ?: rhs?.errorMessage,
        stackTrace = lhs?.stackTrace ?: rhs?.stackTrace,
    )
}

class FinishedTask(
    val instanceID: TaskInstanceID,
    val taskName: TaskName,
    val status: Status,
    val finishedAt: OffsetDateTime,
    val additionalInfo: AdditionalInfo?,
)

@Serializable
data class StatusEntry(
    val instance: TaskInstanceID,
    val status: Status,
    @Serializable(KOffsetDateTimeSerializer::class) val scheduledAt: OffsetDateTime,
    @Serializable(KOffsetDateTimeSerializer::class) val enqueuedAt: OffsetDateTime? = null,
    @Serializable(KOffsetDateTimeSerializer::class) val startedAt: OffsetDateTime? = null,
    @Serializable(KOffsetDateTimeSerializer::class) val finishedAt: OffsetDateTime? = null,
    val info: AdditionalInfo? = null,
)


interface StatusRepository {
    fun insert(instance: TaskInstanceID, moment: OffsetDateTime)
    fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime,
                     info: AdditionalInfo? = null)
    fun finishedTasks(): List<FinishedTask>
    fun taskHistory(taskName: TaskName, from: OffsetDateTime = OffsetDateTime.MIN,
                    to: OffsetDateTime = OffsetDateTime.MAX): List<StatusEntry>
    fun history(from: OffsetDateTime = OffsetDateTime.MIN,
                to: OffsetDateTime = OffsetDateTime.MAX): List<Pair<TaskName, StatusEntry>>
}