package org.schedo.repository.inmemory

import org.schedo.repository.ExecutionsRepository
import org.schedo.repository.TaskStatus
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

data class ExecutionEntry(
    val currentInstance: TaskInstanceID,
    val status: TaskStatus,
    val retryCount: Int = 0,
    val cancelled: Boolean = false,
    val cancelledAt: OffsetDateTime? = null
)
class InMemoryExecutions : ExecutionsRepository {
    private val executions = ConcurrentHashMap<TaskName, ExecutionEntry>()

    override fun setRetryCount(task: TaskName, value: Int) {
        executions.computeIfPresent(task){ _, executionEntry ->
            executionEntry.copy(retryCount = value)
        }
    }

    override fun addRetryCount(task: TaskName, delta: Int) {
        executions.computeIfPresent(task){ _, value ->
            value.copy(retryCount = value.retryCount + delta)
        }
    }

    override fun getRetryCount(task: TaskName): UInt =
        (executions[task]?.retryCount ?: 0).toUInt()

    override fun updateStatus(task: TaskName, instanceID: TaskInstanceID, status: TaskStatus) {
        if (status != TaskStatus.RUNNING && status != TaskStatus.FINISHED) {
            return
        }
        if (status == TaskStatus.FINISHED && executions[task]?.currentInstance != instanceID) {
            return
        }

        executions.compute(task) { _, existing ->
            if (status == TaskStatus.FINISHED && existing != null && existing.currentInstance != instanceID) {
                // outdated
                return@compute existing
            }

            existing
                ?.copy(currentInstance = instanceID, status = status)
                ?: ExecutionEntry(
                currentInstance = instanceID,
                status = status,
            )
        }
    }

    override fun cancel(task: TaskName, moment: OffsetDateTime): Boolean {
        var cancelled = false
        executions.computeIfPresent(task){ _, value ->
            if (!value.cancelled) {
                cancelled = true
                value.copy(cancelled = true, cancelledAt = moment)
            } else {
                value
            }
        }
        return cancelled
    }

    override fun whenCancelled(task: TaskName): OffsetDateTime? {
        var cancelledAt: OffsetDateTime? = null
        executions.computeIfPresent(task){ _, value ->
            if (value.cancelled) {
                cancelledAt = value.cancelledAt
                value.copy(status = TaskStatus.CANCELLED)
            } else {
                value
            }
        }
        return cancelledAt
    }

    override fun tryResume(task: TaskName): Boolean {
        var resumed = false
        executions.computeIfPresent(task){ _, value ->
            if (value.status == TaskStatus.FINISHED || value.status == TaskStatus.CANCELLED) {
                resumed = true
                value.copy(status = TaskStatus.RESUMED, cancelled = false)
            } else {
                value
            }
        }
        return resumed
    }

    override fun getStatusAndCancelled(task: TaskName): Pair<TaskStatus, Boolean>? {
        val execution = executions[task] ?: return null
        return Pair(execution.status, execution.cancelled)
    }
}