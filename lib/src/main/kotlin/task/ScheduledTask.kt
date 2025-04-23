package task

import scheduler.Scheduler
import util.getRandomString
import java.time.OffsetTime

open class ScheduledTask(
    val func: () -> Unit,
    val executionTime: OffsetTime,
    val name: String = getRandomString(10),
    ) {
    open val completionHandler: (scheduler: Scheduler) -> Unit = {}
}