import java.util.concurrent.Executors
import kotlin.time.Duration

import kotlin.time.TimeSource

class Scheduler {
    val timeSource = TimeSource.Monotonic
    val repository = InMemoryRepository()
    val executor = Executors.newCachedThreadPool()

    fun scheduleAfter(duration: Duration, func: () -> Unit) {
        repository.add(ScheduledTask(func, timeSource.markNow() + duration))
    }

    fun run() {
        while (true) {
            val tasksToRun = repository.pickDueExecution()
            for (task in tasksToRun) {
                executor.submit(task.func)
            }
        }
    }
}