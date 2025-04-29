package task

import components.TaskResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import repository.Repository
import repository.ScheduledTaskInstance
import repository.TaskResult
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class TaskManager(
    private val repository: Repository,
    private val taskResolver: TaskResolver = TaskResolver(),
) {

    fun pickDueNow(): List<TaskInstance> =
        repository.tasksRepository.pickTaskInstancesDue(OffsetDateTime.now())
            .mapNotNull { (id, name) -> taskResolver.getTask(name)?.let { TaskInstance(id, it) } }


    fun updateTaskStatus(taskName: TaskName, result: TaskResult) {
        // TODO: update task status in DB
        when (result) {
            is TaskResult.Failed -> {}
            is TaskResult.Success -> {}
        }
    }

    fun schedule(taskName: TaskName, moment: OffsetDateTime) {
        val taskInstanceID = TaskInstanceID(UUID.randomUUID())
        logger.info{"schedule taskInstance $taskInstanceID of task $taskName to execute at $moment"}
        repository.tasksRepository.add(ScheduledTaskInstance(taskInstanceID, taskName, moment))
    }

    fun schedule(task: Task, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        schedule(task.name, moment)
    }
}