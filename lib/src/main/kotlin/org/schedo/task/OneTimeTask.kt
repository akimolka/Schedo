package org.schedo.task

import org.schedo.manager.ITaskManager
import org.schedo.retry.RetryPolicy

abstract class OneTimeTask(
    name: TaskName,
    retryPolicy: RetryPolicy? = null,
    successHandler: (ITaskManager) -> Unit = {},
    exceptionHandler: (Exception, ITaskManager) -> Unit = { _, _ ->},
    failureHandler: (ITaskManager) -> Unit = {},
) : Task(
    name,
    retryPolicy,
    successHandlers = listOf(successHandler),
    exceptionHandlers = listOf(exceptionHandler),
    failureHandlers = listOf(failureHandler),
)