package task

import Scheduler
import kotlin.time.TimeMark

open class ScheduledTask(val func: () -> Unit, val executionTime: TimeMark) {
    open val completionHandler: (scheduler: Scheduler) -> Unit = {println("completed")}
}