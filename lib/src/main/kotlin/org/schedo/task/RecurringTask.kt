package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy

abstract class RecurringTask(
    name: TaskName,
    private val schedule: RecurringSchedule,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(taskManager: TaskManager) {
        val now = taskManager.dateTimeService.now()
        taskManager.schedule(name, schedule.nextExecution(now))
    }
}

