package org.schedo.repository.inmemory

import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes on an entry corresponding a given instance are ordered
 */
class InMemoryStatus(
    val inMemoryJoin: InMemoryJoin
) : StatusRepository {
    private val statuses = ConcurrentHashMap<TaskInstanceID, StatusEntry>()

    override fun insert(instance: TaskInstanceID, scheduledFor: OffsetDateTime, createdAt: OffsetDateTime) {
        statuses[instance] = StatusEntry(instance, Status.SCHEDULED, scheduledFor, createdAt)
    }

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo?) {
        statuses.computeIfPresent(instance) { _, old ->
            when (status) {
                Status.ENQUEUED -> old.copy(status = status, enqueuedAt = moment, info = mergeInfo(old.info, info))
                Status.STARTED -> old.copy(status = status, startedAt = moment, info = mergeInfo(old.info, info))
                Status.COMPLETED, Status.FAILED -> old.copy(status = status, finishedAt = moment, info = mergeInfo(old.info, info))
                else -> old
            }
        }
    }

    override fun finishedTasks(): List<FinishedTask> {
        return statuses.values
            .asSequence()
            .mapNotNull { entry ->
                val finishedAt = entry.finishedAt
                val taskName = inMemoryJoin.taskName(entry.instance)
                if (finishedAt != null && taskName != null) {
                    FinishedTask(
                        instanceID = entry.instance,
                        taskName = taskName,
                        status = entry.status,
                        finishedAt = finishedAt,
                        additionalInfo = entry.info
                    )
                } else {
                    null
                }
            }
            .toList()
    }

    override fun taskHistory(taskName: TaskName, from: OffsetDateTime, to: OffsetDateTime): List<StatusEntry> {
        val instances = inMemoryJoin.taskInstances(taskName)

        return instances
            .mapNotNull { instanceId -> statuses[instanceId] }
            .filter { it.scheduledFor in from..to }
    }

    override fun history(from: OffsetDateTime, to: OffsetDateTime): List<Pair<TaskName, StatusEntry>> {
        return statuses.values
            .asSequence()
            .filter { it.scheduledFor in from..to }
            .mapNotNull { entry ->
                val taskName = inMemoryJoin.taskName(entry.instance)
                if (taskName != null) {
                    Pair(taskName, entry)
                } else {
                    null
                }
            }
            .toList()
    }

    fun getStatus(instance: TaskInstanceID): StatusEntry? {
        return statuses[instance]?.copy()
    }
}