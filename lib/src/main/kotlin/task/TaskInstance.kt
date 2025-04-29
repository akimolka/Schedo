package task

import repository.TaskResult
import scheduler.Scheduler
import java.time.Duration
import java.time.OffsetDateTime
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
        scheduler.taskManager.updateTaskStatusStart(id)
        val timeSpending = measureTimeMillis {
            task.run()
        }
        scheduler.taskManager.updateTaskStatusFinish(id, TaskResult.Success(Duration.ofMillis(timeSpending)))
        task.onCompleted(scheduler)
    } catch (e: Exception) {
        scheduler.taskManager.updateTaskStatusFinish(id, TaskResult.Failed(e))
        task.onFailed(e, scheduler)
    }
}