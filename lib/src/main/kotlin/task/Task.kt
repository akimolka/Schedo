package task

import scheduler.Scheduler

@JvmInline
value class TaskName(val value: String)

/**
 * Общее описание задачи без привязки ко времени исполнения
 * Содержит название, полезную нагрузку и хендлеры
 * При каждом планировании от задачи создаётся её инстанс
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
}