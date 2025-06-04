package org.schedo.task

import kotlinx.serialization.Serializable
import org.schedo.manager.TaskManager
import org.schedo.manager.TaskResult
import org.schedo.retry.RetryPolicy
import java.time.Duration
import java.util.*
import kotlin.system.measureTimeMillis

@JvmInline
@Serializable
value class TaskName(val value: String)

/**
 * General description of a task without reference to its execution time.
 * Contains name, payload and handlers.
 * Each time a task is scheduled, its instance is created,
 * namely a unique TaskInstanceID is generated.
 * @param successHandler - called upon successful completion
 * @param failureHandler - additional action before retrying according to [retryPolicy]
 */
abstract class Task(
    val name: TaskName,
    private val retryPolicy: RetryPolicy? = null,
    private var successHandler: (TaskManager) -> Unit = {},
    private var failureHandler: (Exception, TaskManager) -> Unit = {_, _ ->},
) {
    /**
     * Payload
     */
    abstract fun run()

    /**
     * Called upon successful task completion
     */
    private fun onSuccess(taskManager: TaskManager) {
        successHandler(taskManager)
    }

    private fun retry(e: Exception, taskManager: TaskManager) {
        if (retryPolicy != null) {
            val failedCount = taskManager.failedCount(name, retryPolicy.maxRetries)
            val delay = retryPolicy.getNextDelay(failedCount)
            if (delay != null) {
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(name, now + delay)
            }
        }
    }

    /**
     * Called upon task failure, i.e. when exception is caught
     */
    private fun onFailure(e: Exception, taskManager: TaskManager) {
        failureHandler(e, taskManager)
        retry(e, taskManager)
    }

    fun onEnqueued(id: TaskInstanceID, taskManager: TaskManager) {
        taskManager.updateTaskStatusEnqueued(id)
    }

    fun exec(id: TaskInstanceID, taskManager: TaskManager) = try {
        taskManager.updateTaskStatusStarted(id)
        val timeSpending = measureTimeMillis {
            run()
        }
        onSuccess(taskManager)
        taskManager.updateTaskStatusFinished(id, TaskResult.Success(Duration.ofMillis(timeSpending)))
    } catch (e: Exception) {
        onFailure(e, taskManager)
        taskManager.updateTaskStatusFinished(id, TaskResult.Failed(e))
    }
}

/**
 * TaskInstanceID is unique among TaskInstanceIDs of all instances of all tasks
 */
@JvmInline
@Serializable
value class TaskInstanceID(val value: String)

data class TaskInstanceName(val id: TaskInstanceID, val name: TaskName)

/**
 * TaskInstance represents a single Task execution.
 * An instance corresponds to exactly one row in SchedoTasks and SchedoStatus.
 */
data class TaskInstance(val id: TaskInstanceID, val task: Task)