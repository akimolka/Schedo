package org.schedo.repository.inmemory

import org.schedo.repository.ExecutionsRepository
import org.schedo.repository.TaskStatus
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

data class ExecutionEntry(
    val retryCount: Int = 0,
)
class InMemoryExecutions : ExecutionsRepository {
    private val executions = ConcurrentHashMap<TaskName, ExecutionEntry>()

    override fun setRetryCount(task: TaskName, value: Int) {
        executions[task] = ExecutionEntry(value)
    }

    override fun addRetryCount(task: TaskName, delta: Int) {
        executions.computeIfPresent(task){ _, value ->
            value.copy(retryCount = value.retryCount + delta)
        }
    }

    override fun getRetryCount(task: TaskName): UInt =
        (executions[task]?.retryCount ?: 0).toUInt()

    override fun updateStatus(task: TaskName, instanceID: TaskInstanceID, status: TaskStatus) {
        TODO("Not yet implemented")
    }

    override fun cancel(task: TaskName, moment: OffsetDateTime): Boolean {
        TODO("Not yet implemented")
    }

    override fun clearCancelled(task: TaskName): Boolean {
        TODO("Not yet implemented")
    }

    override fun whenCancelled(task: TaskName): OffsetDateTime? {
        TODO("Not yet implemented")
    }

    override fun tryResume(task: TaskName): Boolean {
        TODO("Not yet implemented")
    }

    override fun getStatusAndCancelled(task: TaskName): Pair<TaskStatus, Boolean>? {
        TODO("Not yet implemented")
    }
}