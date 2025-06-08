package org.schedo.manager

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.task.Task
import org.schedo.task.TaskInstance
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.repository.*
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import java.time.Duration

private val logger = KotlinLogging.logger {}

sealed interface TaskResult {
    class Success(val spendingTime: Duration) : TaskResult
    class Failed(val e: Exception) : TaskResult
}

fun TaskResult.toStatus(): Status = when(this) {
    is TaskResult.Failed -> Status.FAILED
    is TaskResult.Success -> Status.COMPLETED
}

class TaskManager(
    private val tasksRepository: TasksRepository,
    private val statusRepository: StatusRepository,
    private val executionsRepository: ExecutionsRepository,
    val taskResolver: TaskResolver = TaskResolver(),
    val dateTimeService: DateTimeService = DefaultDateTimeService(),
) {

    fun pickDueNow(): List<TaskInstance> =
        tasksRepository.pickTaskInstancesDue(dateTimeService.now())
            .mapNotNull { (id, name) -> taskResolver.getTask(name)?.let {
                it.onEnqueued(id, this)
                TaskInstance(id, it)
            } }

    fun updateTaskStatusEnqueued(taskInstanceID: TaskInstanceID) {
        statusRepository.updateStatus(Status.ENQUEUED, taskInstanceID, dateTimeService.now())
    }

    fun updateTaskStatusStarted(taskInstanceID: TaskInstanceID) {
        statusRepository.updateStatus(Status.STARTED, taskInstanceID, dateTimeService.now())
    }

    fun updateTaskStatusFinished(taskInstanceID: TaskInstanceID, result: TaskResult) {
        when (result) {
            is TaskResult.Failed -> {
                logger.error{"taskInstance $taskInstanceID failed with ${result.e}"}

                val info = AdditionalInfo(
                    errorMessage = result.e.message ?: "Unknown error",
                    stackTrace = result.e.stackTraceToString()
                )
                statusRepository.updateStatus(result.toStatus(), taskInstanceID, dateTimeService.now(), info)
            }
            is TaskResult.Success -> {
                statusRepository.updateStatus(result.toStatus(), taskInstanceID, dateTimeService.now())
            }
        }
    }

    /**
     * Schedules task with [taskName] to execute at [moment].
     * Must be called for task rescheduling either after a fall
     * or for the second and subsequent runs of a recurring task.
     * The first scheduling of a task must be done via the overload that accepts [Task].
     * @param taskInstanceID specifies id for this run. Do not set it manually unless you know what you are doing.
     * The default value is a random UUID. [taskInstanceID] of the first run of a task is equal to the task's name.
     */
    fun schedule(
        taskName: TaskName, moment: OffsetDateTime,
        isRetry: Boolean = false,
        taskInstanceID: TaskInstanceID = TaskInstanceID(UUID.randomUUID().toString())) {
        val added = tasksRepository.add(ScheduledTaskInstance(taskInstanceID, taskName, moment))
        if (added) {
            logger.info{"Added to the repository: taskInstance $taskInstanceID of task $taskName to execute at $moment"}
            statusRepository.insert(taskInstanceID, moment)
            if (isRetry) executionsRepository.increaseRetryCount(taskName)
            else executionsRepository.resetRetryCount(taskName)
        } else {
            logger.warn{"Did not add to the repository: taskInstance $taskInstanceID of task $taskName"}
        }
    }

    /**
     * Schedules [task] to execute at [moment].
     * Must be called when and only when the task is scheduled for the first time.
     * Subsequent reschedules must use the overload which accepts [TaskName].
     */
    fun schedule(task: Task, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        val firstInstanceID = TaskInstanceID(task.name.value)
        schedule(task.name, moment, false, firstInstanceID)
    }

    /**
     * Returns a number of fails after last success.
     * Note that setting [limit] to zero will result in zero return value,
     * but will not prevent a database query.
     * @param limit limits the number of fails. In other words,
     * minimum of limit and number of fails after last success is returned.
     */
    fun failedCount(taskName: TaskName, limit: UInt) =
        executionsRepository.getRetryCount(taskName)
}