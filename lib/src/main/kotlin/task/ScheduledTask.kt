package task

import scheduler.Scheduler
import java.time.OffsetTime

open class ScheduledTask(val func: () -> Unit, val executionTime: OffsetTime) {
    val name = util.getRandomString(10)
    open val completionHandler: (scheduler: Scheduler) -> Unit = {println("completed")}
}