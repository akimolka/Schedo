package repository

import task.ScheduledTask
import java.time.OffsetTime

interface Repository {
    fun add(task: ScheduledTask)
    fun pickDue(timePoint: OffsetTime): List<ScheduledTask>
}