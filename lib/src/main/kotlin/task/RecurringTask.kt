package task

import scheduler.Scheduler
import util.getRandomString
import java.time.OffsetTime
import java.time.temporal.TemporalAmount

class RecurringTask(
    func: () -> Unit,
    executionTime: OffsetTime,
    period: TemporalAmount,
    name: String = getRandomString(10)
    ) : ScheduledTask(func, executionTime, name) {
    override val completionHandler = {scheduler: Scheduler -> scheduler.scheduleRecurring(period, func)}
}