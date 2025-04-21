import task.ScheduledTask
import java.time.OffsetTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTask>()

    fun add(task: ScheduledTask) {
        tasks.add(task)
    }

    @Synchronized
    fun pickDue(timePoint: OffsetTime): List<ScheduledTask> { // передавать время
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked)
        return picked
    }
}