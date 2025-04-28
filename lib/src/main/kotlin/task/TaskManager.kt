package task

import components.TaskResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import repository.TasksRepository
import repository.TaskEntity
import repository.TaskResult
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

class TaskManager(
    private val repository: TasksRepository,
    private val taskResolver: TaskResolver = TaskResolver(),
) {

    fun pickDueNow(): List<Task> =
        repository.pickTaskNamesDue(OffsetDateTime.now()).mapNotNull { taskResolver.getTask(it) }


    fun updateTaskStatus(taskName: TaskName, result: TaskResult) {
        // TODO: update task status in DB
        when (result) {
            is TaskResult.Failed -> {}
            is TaskResult.Success -> {}
        }
    }

    fun schedule(taskName: TaskName, moment: OffsetDateTime) {
        logger.info{"schedule task $taskName to execute at $moment"}
        repository.add(TaskEntity(taskName, moment))
    }

    fun schedule(task: Task, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        schedule(task.name, moment)
    }
}