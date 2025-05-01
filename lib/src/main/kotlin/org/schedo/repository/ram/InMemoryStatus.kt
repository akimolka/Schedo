package org.schedo.repository.ram

import org.schedo.repository.Status
import org.schedo.repository.StatusRepository
import org.schedo.repository.TaskResult
import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

class StatusEntry(
    val status: Status,
    val scheduledAt: OffsetDateTime?,
    val enqueuedAt: OffsetDateTime? = null,
    val startedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    val result: TaskResult? = null,
    )

class InMemoryStatus : StatusRepository {
    // TODO is it correct usage?
    private val statuses = ConcurrentHashMap<TaskInstanceID, StatusEntry>()

    override fun schedule(instance: TaskInstanceID, moment: OffsetDateTime) {
        statuses[instance] = StatusEntry(Status.SCHEDULED, moment)
    }

    override fun enqueue(instance: TaskInstanceID, moment: OffsetDateTime) {
        val statusEntry = statuses[instance]
        statuses[instance] = StatusEntry(
            Status.ENQUEUED,
            scheduledAt = statusEntry?.scheduledAt,
            enqueuedAt = moment
        )
    }

    override fun start(instance: TaskInstanceID, moment: OffsetDateTime) {
        val statusEntry = statuses[instance]
        statuses[instance] = StatusEntry(
            Status.STARTED,
            scheduledAt = statusEntry?.scheduledAt,
            enqueuedAt = statusEntry?.enqueuedAt,
            startedAt = moment
        )
    }

    override fun finish(instance: TaskInstanceID, result: TaskResult, moment: OffsetDateTime) {
        val statusEntry = statuses[instance]
        statuses[instance] = StatusEntry(
            when (result) {
                is TaskResult.Success -> Status.COMPLETED
                is TaskResult.Failed -> Status.FAILED
            },
            scheduledAt = statusEntry?.scheduledAt,
            enqueuedAt = statusEntry?.enqueuedAt,
            startedAt = statusEntry?.startedAt,
            result = result,
        )
    }
}