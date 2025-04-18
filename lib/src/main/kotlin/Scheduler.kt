import kotlin.time.Duration
import kotlin.collections.mutableListOf
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Scheduler {
    val timeSource = TimeSource.Monotonic
    var repository = InMemoryRepository()
    fun scheduleAfter(duration: Duration, func: () -> Unit) {
        repository.add(ScheduledTask(func, timeSource.markNow() + duration))
    }
    fun run() {
        while (true) {
            val tasksToRun = repository.pickDueExecution()
            for (task in tasksToRun) {
                task.func()
            }
        }
    }
}