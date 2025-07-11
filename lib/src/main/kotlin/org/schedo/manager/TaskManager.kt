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

interface ITaskManager {
    fun pickDueNow(): List<TaskInstance>
    fun updateTaskStatusStarted(taskInstanceID: TaskInstanceID)
    fun updateTaskStatusFinished(taskName: TaskName, taskInstanceID: TaskInstanceID, result: TaskResult)
    fun schedule(taskName: TaskName, moment: OffsetDateTime, isRetry: Boolean = false,
                 taskInstanceID: TaskInstanceID = TaskInstanceID(UUID.randomUUID().toString()))
    fun schedule(task: Task, moment: OffsetDateTime)
    fun failedCount(taskName: TaskName, limit: UInt): UInt
    fun resume(taskName: TaskName): Boolean
    fun forceResume(taskName: TaskName)
    val taskResolver: TaskResolver
    val dateTimeService: DateTimeService
}

class TaskManager(
    private val tasksRepository: TasksRepository,
    private val statusRepository: StatusRepository,
    private val executionsRepository: ExecutionsRepository,
    private val tm: TransactionManager,
    override val taskResolver: TaskResolver = TaskResolver(),
    override val dateTimeService: DateTimeService = DefaultDateTimeService(),
) : ITaskManager {

    override fun pickDueNow(): List<TaskInstance> =
        tm.transaction {
            tasksRepository.pickTaskInstancesDue(dateTimeService.now())
                .mapNotNull { (id, name) ->
                    val cancelMoment = executionsRepository.whenCancelled(name)
                    if (cancelMoment != null) {
                        statusRepository.updateStatus(Status.CANCELLED, id, cancelMoment)
                        null
                    } else {
                        taskResolver.getTask(name)?.let {
                            statusRepository.updateStatus(Status.ENQUEUED, id, dateTimeService.now())
                            TaskInstance(id, it)
                        }
                    }
                }
        }

    override fun updateTaskStatusStarted(taskInstanceID: TaskInstanceID) =
        tm.transaction {
            statusRepository.updateStatus(Status.STARTED, taskInstanceID, dateTimeService.now())
        }

    override fun updateTaskStatusFinished(taskName: TaskName, taskInstanceID: TaskInstanceID, result: TaskResult) {
        val info = when (result) {
            is TaskResult.Failed -> AdditionalInfo(
                    errorMessage = result.e.message ?: "Unknown error",
                    stackTrace = result.e.stackTraceToString()
            )
            is TaskResult.Success -> null
        }

        tm.transaction {
            statusRepository.updateStatus(result.toStatus(), taskInstanceID, dateTimeService.now(), info)
            executionsRepository.updateStatus(taskName, taskInstanceID, TaskStatus.FINISHED)
        }
    }

    private fun scheduleImpl(
        taskName: TaskName, moment: OffsetDateTime,
        isRetry: Boolean, taskInstanceID: TaskInstanceID) {
        val added = tasksRepository.add(ScheduledTaskInstance(taskInstanceID, taskName, moment))
        if (added) {
            logger.info{"Added to the repository: taskInstance $taskInstanceID of task $taskName to execute at $moment"}
            statusRepository.insert(taskInstanceID, moment, dateTimeService.now())
            executionsRepository.updateStatus(taskName, taskInstanceID, TaskStatus.RUNNING)
            if (isRetry) executionsRepository.increaseRetryCount(taskName)
            else executionsRepository.resetRetryCount(taskName)
        } else {
            logger.warn{"Did not add to the repository: taskInstance $taskInstanceID of task $taskName"}
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
    override fun schedule(
        taskName: TaskName, moment: OffsetDateTime, isRetry: Boolean, taskInstanceID: TaskInstanceID) {
        tm.transaction {
            scheduleImpl(taskName, moment, isRetry, taskInstanceID)
        }
    }

    /**
     * Schedules [task] to execute at [moment].
     * Must be called when and only when the task is scheduled for the first time.
     * Subsequent reschedules must use the overload which accepts [TaskName].
     */
    override fun schedule(task: Task, moment: OffsetDateTime) {
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
    override fun failedCount(taskName: TaskName, limit: UInt) =
        tm.transaction {
            executionsRepository.getRetryCount(taskName)
        }

    override fun resume(taskName: TaskName): Boolean =
        tm.transaction {
            val resumed = executionsRepository.tryResume(taskName)
            if (resumed) {
                val taskInstanceID = TaskInstanceID(UUID.randomUUID().toString())
                val now = dateTimeService.now()
                scheduleImpl(taskName, now, false, taskInstanceID)
                true
            } else {
                false
            }
        }

    override fun forceResume(taskName: TaskName) =
        tm.transaction {
            executionsRepository.forceResume(taskName)
            val taskInstanceID = TaskInstanceID(UUID.randomUUID().toString())
            val now = dateTimeService.now()
            scheduleImpl(taskName, now, false, taskInstanceID)
        }
}