package org.schedo.task

import org.schedo.scheduler.Scheduler
import org.schedo.retry.RetryPolicy

abstract class OneTimeTask (
    name: TaskName,
    retryPolicy: RetryPolicy? = null,
) : Task(name, retryPolicy) {
    override fun onCompleted(scheduler: Scheduler) {}
}