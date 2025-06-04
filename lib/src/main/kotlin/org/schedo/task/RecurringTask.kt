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
    exceptionHandler: (Exception, TaskManager) -> Unit = {_, _ ->},
    failureHandler: (TaskManager) -> Unit = {},
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
    exceptionHandler = exceptionHandler,
    failureHandler = failureHandler,
    )

