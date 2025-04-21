package task

import Scheduler
import java.time.OffsetTime

open class ScheduledTask(val func: () -> Unit, val executionTime: OffsetTime) {
    open val completionHandler: (scheduler: Scheduler) -> Unit = {println("completed")}
}