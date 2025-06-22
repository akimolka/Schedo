package org.schedo.server
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.schedo.manager.ITaskManager
import org.schedo.repository.*
import org.schedo.task.TaskName
import org.schedo.util.KOffsetDateTimeSerializer
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

@Serializable
class FailedTaskInfo (
    val name: TaskName,
    @Serializable(KOffsetDateTimeSerializer::class) val lastFailure: OffsetDateTime,
    val recovered: Boolean,
    val errorMessage: String,
)

@Serializable
data class AggrTaskInfo (
    val name: TaskName,
    val successCount: Int,
    val failureCount: Int,
    val lastExecutionTime: @Serializable(KOffsetDateTimeSerializer::class) OffsetDateTime?,
)

@Serializable
class DetailedTaskInfo (
    val history: List<StatusEntry>,
    val executionStatus: TaskStatus?,
    val isCancelled: Boolean?,
)

class TaskController(
    private val tasksRepository: TasksRepository,
    private val statusRepository: StatusRepository,
    private val executionsRepository: ExecutionsRepository,
    private val tm: TransactionManager,
    private val taskManager: ITaskManager, // TODO
) {
    fun countScheduledTasks(due: OffsetDateTime): Int =
        tm.transaction {
            tasksRepository.countTaskInstancesDue(due)
        }

    fun scheduledTasks(due: OffsetDateTime = OffsetDateTime.MAX): List<ScheduledTaskInstance> =
        tm.transaction {
            tasksRepository.listTaskInstancesDue(due).sortedBy { it.executionTime }
        }

    fun failedTasks(): List<FailedTaskInfo> {
        val allFinished = tm.transaction {
            statusRepository.finishedTasks()
        }

        val saneFinished = allFinished.filter {
            when (it.status) {
                Status.COMPLETED, Status.FAILED -> true
                else -> {
                    logger.warn { "Ignoring unexpected status ${it.status} for task ${it.taskName}" }
                    false
                }
            }
        }

        return saneFinished
            .groupBy { it.taskName }
            .mapNotNull { (taskName, entries) ->
                val sorted = entries.sortedByDescending { it.finishedAt }

                if (sorted.none { it.status == Status.FAILED }) return@mapNotNull null

                val mostRecent = sorted.first()
                val recovered = mostRecent.status == Status.COMPLETED

                val lastFailEntry = sorted.first { it.status == Status.FAILED }

                val msg = lastFailEntry
                    .additionalInfo?.errorMessage
                    .orEmpty()

                FailedTaskInfo(
                    name        = taskName,
                    lastFailure = lastFailEntry.finishedAt,
                    recovered   = recovered,
                    errorMessage= msg
                )
            }
            .sortedByDescending { it.lastFailure }
    }

    fun taskHistory(taskName: TaskName): DetailedTaskInfo =
        tm.transaction {
            val statusAndCancelled = executionsRepository.getStatusAndCancelled(taskName)
            DetailedTaskInfo(
                history = statusRepository.taskHistory(taskName).sortedByDescending { it.createdAt },
                executionStatus = statusAndCancelled?.first,
                isCancelled = statusAndCancelled?.second,
            )
        }

    fun tasks(from: OffsetDateTime, to: OffsetDateTime, taskName: TaskName?, status: Status?): List<AggrTaskInfo> {
        // TODO How to use status?
        return if (taskName == null) {
            tm.transaction { statusRepository.history(from, to) }
                .groupBy { it.first }
                .map { (name, pairs) ->
                    val entries: List<StatusEntry> = pairs.map { it.second }
                    collectTaskInfo(name, entries)
                }
        } else {
            tm.transaction {
                listOf(collectTaskInfo(taskName, statusRepository.taskHistory(taskName, from, to)))
            }
        }
    }

    private fun collectTaskInfo(taskName: TaskName, taskEntries: List<StatusEntry>): AggrTaskInfo {
        val successCount = taskEntries.count { it.status == Status.COMPLETED }
        val failureCount = taskEntries.count { it.status == Status.FAILED }

        val lastExecutionTime = taskEntries
            .mapNotNull { it.startedAt }
            .maxOrNull()

        return AggrTaskInfo(
            name              = taskName,
            successCount      = successCount,
            failureCount      = failureCount,
            lastExecutionTime = lastExecutionTime
        )
    }

    /**
     * @return whether action was successful
     */
    fun resumeTask(task: TaskName): Boolean =
        taskManager.resume(task)

    fun forceResumeTask(task: TaskName) =
        taskManager.forceResume(task)

    fun cancelTask(task: TaskName): Boolean {
        val now = taskManager.dateTimeService.now()
        return tm.transaction { executionsRepository.cancel(task, now) }
    }
}