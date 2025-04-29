package task

import repository.TaskResult
import scheduler.Scheduler
import java.time.Duration
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * TaskInstanceID уникальный среди id всех инстансов всех задач
 */
@JvmInline
value class TaskInstanceID(val value: UUID)

data class TaskInstanceFullName(val id: TaskInstanceID, val name: TaskName)

/**
 * TaskInstance представляет собой одно исполнение Task
 * Инстансу соответствует ровно одна строка в SchedoTasks и SchedoStatus
 */
class TaskInstance(
    private val id: TaskInstanceID,
    private val task: Task,
) {
    fun exec(scheduler: Scheduler) = try {
        val timeSpending = measureTimeMillis {
            task.run()
        }
        scheduler.taskManager.updateTaskStatus(task.name, TaskResult.Success(Duration.ofMillis(timeSpending)))
        task.onCompleted(scheduler)
    } catch (e: Exception) {
        scheduler.taskManager.updateTaskStatus(task.name, TaskResult.Failed(e))
        task.onFailed(e, scheduler)
    }
}