package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy

/**
 * @param successHandler - additional action before rescheduling according to [schedule]
 * @param failureHandler - additional action before retrying according to [retryPolicy]
 */
abstract class RecurringTask(
    name: TaskName,
    private val schedule: RecurringSchedule,
    retryPolicy: RetryPolicy? = null,
    successHandler: (TaskManager) -> Unit = {},
    failureHandler: (Exception, TaskManager) -> Unit = {_, _ ->},
) : Task(
    name,
    retryPolicy,
    successHandler = { taskManager ->
        // additional action
        successHandler(taskManager)
        // rescheduling
        val now = taskManager.dateTimeService.now()
        taskManager.schedule(name, schedule.nextExecution(now))
    },
    failureHandler = failureHandler,
    )

