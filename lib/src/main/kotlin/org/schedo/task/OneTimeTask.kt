package org.schedo.task

import org.schedo.scheduler.Scheduler

abstract class OneTimeTask (
    name: TaskName,
) : Task(name) {
    override fun onCompleted(scheduler: Scheduler) {}

    override fun onFailed(e: Exception, scheduler: Scheduler) {}
}