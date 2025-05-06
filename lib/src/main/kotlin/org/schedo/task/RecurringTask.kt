package org.schedo.task

import org.schedo.manager.TaskManager
import com.cronutils.model.time.ExecutionTime
import org.schedo.retry.RetryPolicy
import org.schedo.util.nextExecution
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

abstract class RecurringCronTask(
    name: TaskName,
    private val executionTime: ExecutionTime,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(taskManager: TaskManager) {
        val now = taskManager.dateTimeService.now()
        val next = nextExecution(executionTime, now)
        taskManager.schedule(name, next)
    }
}