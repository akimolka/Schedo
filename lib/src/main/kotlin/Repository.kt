import task.ScheduledTask
import java.time.OffsetTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTask>()


    fun add(task: ScheduledTask) {
        tasks.add(task)
    }

    @Synchronized
    fun pickDueExecution(): List<ScheduledTask> { // передавать время
        val now = OffsetTime.now()
        val picked = tasks.filter { it.executionTime.isBefore(now) }
        tasks.removeAll(picked)
        return picked
    }
}