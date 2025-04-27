package repository

import task.ScheduledTask
import java.time.OffsetTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryRepository: Repository {
    private val tasks = CopyOnWriteArrayList<ScheduledTask>()

    override fun add(task: ScheduledTask) {
        tasks.add(task)
    }

    @Synchronized
    override fun pickDue(timePoint: OffsetTime): List<ScheduledTask> {
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked)
        return picked
    }
}