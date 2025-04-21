package task

import Scheduler
import java.time.OffsetTime
import java.time.temporal.TemporalAmount

class RecurringTask(func: () -> Unit, executionTime: OffsetTime, period: TemporalAmount) : ScheduledTask(func, executionTime) {
    override val completionHandler = {scheduler: Scheduler -> scheduler.scheduleRecurring(period, func)}
}