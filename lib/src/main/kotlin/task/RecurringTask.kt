package task

import scheduler.Scheduler
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.temporal.TemporalAmount

abstract class RecurringTask(
    name: TaskName,
    private val period: TemporalAmount
) : ScheduledTask(name) {
    override fun onCompleted(scheduler: Scheduler) {
        scheduler.taskManager.schedule(name, OffsetDateTime.now().plus(period))
    }

    override fun onFailed(e: Exception, scheduler: Scheduler) {
        // можно посчитать количество ретраев например, не планировать задачу
        scheduler.taskManager.schedule(name, OffsetDateTime.now().plus(period))
    }
}