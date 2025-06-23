package org.schedo.task

import org.schedo.manager.ITaskManager
import org.schedo.retry.RetryPolicy

/**
 * @param successHandler - additional action before rescheduling according to [schedule]
 * @param exceptionHandler - additional action before retrying according to [retryPolicy]
 * @param failureHandler - action when the limit of retry attempts is reached
 */
abstract class RecurringTask(
    name: TaskName,
    private val schedule: RecurringSchedule,
    retryPolicy: RetryPolicy? = null,
    successHandler: (ITaskManager) -> Unit = {},
    exceptionHandler: (Exception, ITaskManager) -> Unit = {_, _ ->},
    failureHandler: (ITaskManager) -> Unit = {},
) : Task(
    name,
    retryPolicy,
    successHandlers = listOf(
        successHandler,
        { taskManager ->
            // rescheduling
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(name, schedule.nextExecution(now))
        }),
    exceptionHandlers = listOf(exceptionHandler),
    failureHandlers = listOf(failureHandler),
    )

