package org.schedo.manager

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TaskResult
import org.schedo.task.Task
import org.schedo.task.TaskInstance
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.repository.*
import org.schedo.repository.inmemory.*
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService

private val logger = KotlinLogging.logger {}

class TaskManager(
    private val tasksRepository: TasksRepository = InMemoryTasks(),
    private val statusRepository: StatusRepository = InMemoryStatus(),
    private val taskResolver: TaskResolver = TaskResolver(),
    private val dateTimeService: DateTimeService = DefaultDateTimeService(),
) {

    fun pickDueNow(): List<TaskInstance> =
        tasksRepository.pickTaskInstancesDue(dateTimeService.now())
            .mapNotNull { (id, name) -> taskResolver.getTask(name)?.let {
                statusRepository.enqueue(id, dateTimeService.now())
                TaskInstance(id, it)
            } }

    fun updateTaskStatusStart(taskInstanceID: TaskInstanceID) {
        statusRepository.start(taskInstanceID, dateTimeService.now())
    }

    fun updateTaskStatusFinish(taskInstanceID: TaskInstanceID, result: TaskResult) {
        statusRepository.finish(taskInstanceID, result, dateTimeService.now())
        when (result) {
            is TaskResult.Failed -> logger.error{"taskInstance $taskInstanceID failed with ${result.e}"}
            is TaskResult.Success -> {}
        }
    }

    fun schedule(taskName: TaskName, moment: OffsetDateTime) {
        val taskInstanceID = TaskInstanceID(UUID.randomUUID())
        logger.info{"schedule taskInstance $taskInstanceID of task $taskName to execute at $moment"}
        tasksRepository.add(ScheduledTaskInstance(taskInstanceID, taskName, moment))
        statusRepository.schedule(taskInstanceID, moment)
    }

    fun schedule(task: Task, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        schedule(task.name, moment)
    }
}