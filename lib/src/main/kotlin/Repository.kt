import task.ScheduledTask
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTask>()


    fun add(task: ScheduledTask) {
        tasks.add(task)
    }

    @Synchronized
    fun pickDueExecution(): List<ScheduledTask> { // передавать время
        val picked = tasks.filter { it.executionTime.hasPassedNow() }
        tasks.removeAll(picked)
        return picked
    }
}