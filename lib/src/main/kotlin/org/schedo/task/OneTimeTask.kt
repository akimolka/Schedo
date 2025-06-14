package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy

abstract class OneTimeTask(
    name: TaskName,
    retryPolicy: RetryPolicy? = null,
    successHandler: (TaskManager) -> Unit = {},
    exceptionHandler: (Exception, TaskManager) -> Unit = { _, _ ->},
    failureHandler: (TaskManager) -> Unit = {},
) : Task(
    name,
    retryPolicy,
    successHandlers = listOf(successHandler),
    exceptionHandlers = listOf(exceptionHandler),
    failureHandlers = listOf(failureHandler),
)