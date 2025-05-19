package org.schedo.server
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
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
class TaskInfo (
    val name: TaskName,
    val successCount: Int,
    val failureCount: Int,
    val lastExecutionTime: @Serializable(KOffsetDateTimeSerializer::class) OffsetDateTime?,
)

class TaskController(
    private val tasksRepository: TasksRepository,
    private val statusRepository: StatusRepository,
) {
    fun countScheduledTasks(due: OffsetDateTime): Int {
        return tasksRepository.countTaskInstancesDue(due)
    }

    fun scheduledTasks(due: OffsetDateTime = OffsetDateTime.MAX): List<ScheduledTaskInstance> {
        return tasksRepository.listTaskInstancesDue(due)
    }

    fun failedTasks(): List<FailedTaskInfo> {
        val allFinished = statusRepository.finishedTasks()

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
                    .additionalInfo
                    .errorMessage
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

    fun taskHistory(taskName: TaskName): List<StatusEntry> {
        return statusRepository.taskHistory(taskName)
    }

    fun tasks(from: OffsetDateTime, to: OffsetDateTime, taskName: TaskName?, status: Status?): List<TaskInfo> {
        // TODO How to use status?
        return if (taskName == null) {
            statusRepository.history(from, to)
                .groupBy { it.first }
                .map { (name, pairs) ->
                    val entries: List<StatusEntry> = pairs.map { it.second }
                    collectTaskInfo(name, entries)
                }
        } else {
            listOf(collectTaskInfo(taskName, statusRepository.taskHistory(taskName, from, to)))
        }
    }

    private fun collectTaskInfo(taskName: TaskName, taskEntries: List<StatusEntry>): TaskInfo {
        val successCount = taskEntries.count { it.status == Status.COMPLETED }
        val failureCount = taskEntries.count { it.status == Status.FAILED }

        val lastExecutionTime = taskEntries
            .mapNotNull { it.finishedAt }
            .maxOrNull()

        return TaskInfo(
            name              = taskName,
            successCount      = successCount,
            failureCount      = failureCount,
            lastExecutionTime = lastExecutionTime
        )
    }

    fun finishedTasks(): List<TaskInstanceID /*TaskName, finishedAt, additional info*/> {
        // In additional info include time of execution: finishedAt - startedAt
        // And error if failed: add error field to Status Table?
        return emptyList()
    }

    fun rescheduleTask(task: TaskName) {
        // Button to reschedule finished task
    }

    fun cancelTask(task: TaskName) {
        // no such feature yet
    }
}