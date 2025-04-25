package task

import components.TaskResolver
import repository.Repository
import repository.TaskEntity
import java.time.Duration
import java.time.OffsetDateTime

sealed interface TaskResult {
    class Success(val spendingTime: Duration) : TaskResult
    class Failed(val e: Exception) : TaskResult
}

class TaskManager(
    private val repository: Repository,
    private val taskResolver: TaskResolver = TaskResolver(),
) {

    fun pickDueNow(): List<ScheduledTask> =
        repository.pickTaskNamesDue(OffsetDateTime.now()).mapNotNull { taskResolver.getTask(it) }


    fun updateTaskStatus(taskName: TaskName, result: TaskResult) {
        // TODO: update task status in DB
        when (result) {
            is TaskResult.Failed -> {}
            is TaskResult.Success -> {}
        }
    }

    fun schedule(taskName: TaskName, moment: OffsetDateTime) {
        repository.add(TaskEntity(taskName, moment))
    }

    fun schedule(task: ScheduledTask, moment: OffsetDateTime) {
        taskResolver.addTask(task)
        repository.add(TaskEntity(task.name, moment))
    }
}