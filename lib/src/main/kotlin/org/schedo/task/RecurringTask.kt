package org.schedo.task

import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.Scheduler
import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount

abstract class RecurringTask(
    name: TaskName,
    private val period: TemporalAmount,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(scheduler: Scheduler) {
        val moment = scheduler.dateTimeService.now().plus(period)
        scheduler.taskManager.schedule(name, moment)
    }
}