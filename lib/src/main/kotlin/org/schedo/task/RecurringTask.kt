package org.schedo.task

import org.schedo.scheduler.Scheduler
import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount

abstract class RecurringTask(
    name: TaskName,
    private val period: TemporalAmount
) : Task(name) {
    override fun onCompleted(scheduler: Scheduler) {
        val moment = scheduler.dateTimeService.now().plus(period)
        scheduler.taskManager.schedule(name, moment)
    }

    override fun onFailed(e: Exception, scheduler: Scheduler) {
        // можно посчитать количество ретраев например, не планировать задачу
        val moment = scheduler.dateTimeService.now().plus(period)
        scheduler.taskManager.schedule(name, moment)
    }
}