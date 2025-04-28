package task

import repository.TaskResult
import scheduler.Scheduler
import java.time.Duration
import kotlin.system.measureTimeMillis

@JvmInline
value class TaskName(val value: String)

/**
 * Общее описание задачи без привязки ко времени исполнения
 */
abstract class Task(
    val name: TaskName,
) {
    /**
     * Полезная нагрузка
     */
    abstract fun run()

    /**
     * Вызывается при успешном завершении
     */
    abstract fun onCompleted(scheduler: Scheduler)

    /**
     * Вызывается при неудачном завершении
     */
    abstract fun onFailed(e: Exception, scheduler: Scheduler)

    fun exec(scheduler: Scheduler) = try {
        val timeSpending = measureTimeMillis {
            run()
        }
        scheduler.taskManager.updateTaskStatus(name, TaskResult.Success(Duration.ofMillis(timeSpending)))
        onCompleted(scheduler)
    } catch (e: Exception) {
        scheduler.taskManager.updateTaskStatus(name, TaskResult.Failed(e))
        onFailed(e, scheduler)
    }
}