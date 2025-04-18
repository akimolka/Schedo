package task

import Scheduler
import kotlin.time.Duration
import kotlin.time.TimeMark

class RecurringTask(func: () -> Unit, executionTime: TimeMark, period: Duration) : ScheduledTask(func, executionTime) {
    override val completionHandler = {scheduler: Scheduler -> scheduler.scheduleRecurring(period, func)}
}