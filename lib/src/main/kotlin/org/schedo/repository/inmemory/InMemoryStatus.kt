package org.schedo.repository.inmemory

import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes on an entry corresponding a given instance are ordered
 */
class InMemoryStatus : StatusRepository {
    private val statuses = ConcurrentHashMap<TaskInstanceID, StatusEntry>()

    override fun insert(instance: TaskInstanceID, moment: OffsetDateTime) {
        statuses[instance] = StatusEntry(instance, Status.SCHEDULED, moment)
    }

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo?) {
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
        // It would have been much easier if status table had also TaskName
        TODO("Not yet implemented")
    }

    override fun taskHistory(taskName: TaskName, from: OffsetDateTime, to: OffsetDateTime): List<StatusEntry> {
        TODO("Not yet implemented")
    }

    override fun history(from: OffsetDateTime, to: OffsetDateTime): List<Pair<TaskName, StatusEntry>> {
        TODO("Not yet implemented")
    }

    fun getStatus(instance: TaskInstanceID): StatusEntry? {
        return statuses[instance]?.copy()
    }
}