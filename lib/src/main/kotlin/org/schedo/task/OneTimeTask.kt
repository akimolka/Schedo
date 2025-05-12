package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy

abstract class OneTimeTask (
    name: TaskName,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(taskManager: TaskManager) {}
}