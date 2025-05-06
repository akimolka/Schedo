package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy
import java.time.temporal.TemporalAmount

abstract class RecurringTask(
    name: TaskName,
    private val period: TemporalAmount,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(taskManager: TaskManager) {
        val moment = taskManager.dateTimeService.now().plus(period)
        taskManager.schedule(name, moment)
    }
}