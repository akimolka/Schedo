package org.schedo.repository.inmemory

import org.schedo.repository.ExecutionsRepository
import org.schedo.task.TaskName
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

}