package org.schedo.task

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.schedo.manager.ITaskManager
import org.schedo.manager.TaskResult
import org.schedo.retry.RetryPolicy
import java.time.Duration
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

@JvmInline
@Serializable
value class TaskName(val value: String)

/**
 * General description of a task without reference to its execution time.
 * Contains name, payload and handlers.
 * Each time a task is scheduled, its instance is created,
 * namely a unique TaskInstanceID is generated.
 * @param successHandlers - called in given order upon successful completion
 * @param exceptionHandlers - additional actions before retrying according to [retryPolicy]
 * @param failureHandlers - called in given order when all retry attempts failed
 */
abstract class Task(
    val name: TaskName,
    private val retryPolicy: RetryPolicy? = null,
    var successHandlers: List<(ITaskManager) -> Unit> = emptyList(),
    var exceptionHandlers: List<(Exception, ITaskManager) -> Unit> = emptyList(),
    var failureHandlers: List<(ITaskManager) -> Unit> = emptyList(),
) {
    /**
     * Payload
     */
    abstract fun run()

    /**
     * Called upon successful task completion
     */
    private fun onSuccess(taskManager: ITaskManager) {
        successHandlers.forEach{ it(taskManager) }
    }

    private fun retry(e: Exception, taskManager: ITaskManager) {
        var delay: Duration? = null
        if (retryPolicy != null) {
            val failedCount = taskManager.failedCount(name, retryPolicy.maxRetries)
            delay = retryPolicy.getNextDelay(failedCount)
        }

        if (delay != null) {
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(name, now + delay, isRetry=true)
        } else {
            onFailure(taskManager)
        }
    }

    /**
     * Called upon minor task failure, i.e. when exception is caught
     */
    private fun onException(e: Exception, taskManager: ITaskManager) {
        exceptionHandlers.forEach{ it(e, taskManager) }
        retry(e, taskManager)
    }

    /**
     * Called upon major task failure, i.e. after last failed retry.
     * Note that after last failed retry [onException] is called first,
     * and only then [onFailure].
     */
    private fun onFailure(taskManager: ITaskManager) {
        logger.warn {"Task '${name.value}' has failed completely'"}
        failureHandlers.forEach{ it(taskManager) }
    }

    fun exec(id: TaskInstanceID, taskManager: ITaskManager) = try {
        taskManager.updateTaskStatusStarted(id)
        val timeSpending = measureTimeMillis {
            run()
        }
        onSuccess(taskManager)
        taskManager.updateTaskStatusFinished(name, id, TaskResult.Success(Duration.ofMillis(timeSpending)))
    } catch (e: Exception) {
        onException(e, taskManager)
        taskManager.updateTaskStatusFinished(name, id, TaskResult.Failed(e))
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