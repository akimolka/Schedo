package org.schedo.repository.inmemory

import org.schedo.repository.AdditionalInfo
import org.schedo.repository.FinishedTask
import org.schedo.repository.Status
import org.schedo.repository.StatusRepository
import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

data class StatusEntry(
    val status: Status,
    val scheduledAt: OffsetDateTime?,
    val enqueuedAt: OffsetDateTime? = null,
    val startedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    val info: AdditionalInfo = AdditionalInfo(),
)

/**
 * Writes on an entry corresponding a given instance are ordered
 */
class InMemoryStatus : StatusRepository {
    private val statuses = ConcurrentHashMap<TaskInstanceID, StatusEntry>()

    override fun insert(instance: TaskInstanceID, moment: OffsetDateTime) {
        statuses[instance] = StatusEntry(Status.SCHEDULED, moment)
    }

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo) {
        statuses.computeIfPresent(instance) { _, old ->
            when (status) {
                Status.ENQUEUED -> old.copy(status = status, enqueuedAt = moment, info = old.info.merge(info))
                Status.STARTED -> old.copy(status = status, startedAt = moment, info = old.info.merge(info))
                Status.COMPLETED, Status.FAILED -> old.copy(status = status, finishedAt = moment, info = old.info.merge(info))
                else -> old
            }
        }
    }

    override fun finishedTasks(): List<FinishedTask> {
        TODO("Not yet implemented")
    }

    fun getStatus(instance: TaskInstanceID): StatusEntry? {
        return statuses[instance]?.copy()
    }
}