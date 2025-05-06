package org.schedo.task

import org.schedo.scheduler.Scheduler
import org.schedo.retry.RetryPolicy

abstract class OneTimeTask (
    name: TaskName,
    retryPolicy: RetryPolicy = RetryPolicy.ExpBackoff(),
) : Task(name, retryPolicy) {
    override fun onCompleted(scheduler: Scheduler) {}

    //override fun onFailed(e: Exception, scheduler: Scheduler) {}
}