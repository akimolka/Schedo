package org.schedo.manager

import org.schedo.repository.Repository
import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TaskResult
import org.schedo.task.Task
import org.schedo.task.TaskInstance
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class TaskManager(
    private val repository: Repository,
    private val taskResolver: TaskResolver = TaskResolver(),
) {

    fun pickDueNow(): List<TaskInstance> =
        repository.tasksRepository.pickTaskInstancesDue(OffsetDateTime.now())
            .mapNotNull { (id, name) -> taskResolver.getTask(name)?.let {
                repository.statusRepository.enqueue(id, OffsetDateTime.now())
                TaskInstance(id, it)
            } }

    fun updateTaskStatusStart(taskInstanceID: TaskInstanceID) {
        repository.statusRepository.start(taskInstanceID, OffsetDateTime.now())
    }

    fun updateTaskStatusFinish(taskInstanceID: TaskInstanceID, result: TaskResult) {
        repository.statusRepository.finish(taskInstanceID, result, OffsetDateTime.now())
        when (result) {
            is TaskResult.Failed -> logger.error{"taskInstance $taskInstanceID failed with ${result.e}"}
            is TaskResult.Success -> {}
        }
    }

    fun schedule(taskName: TaskName, moment: OffsetDateTime) {
        val taskInstanceID = TaskInstanceID(UUID.randomUUID())
        logger.info{"schedule taskInstance $taskInstanceID of task $taskName to execute at $moment"}
        repository.tasksRepository.add(ScheduledTaskInstance(taskInstanceID, taskName, moment))
        repository.statusRepository.schedule(taskInstanceID, moment)
    }

    fun schedule(task: Task, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        schedule(task.name, moment)
    }
}