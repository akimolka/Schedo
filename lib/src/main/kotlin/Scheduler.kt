import task.RecurringTask
import task.ScheduledTask
import java.time.OffsetTime
import java.util.concurrent.Executors
import kotlin.time.Duration

import kotlin.time.TimeSource

class Scheduler {
    val timeSource = TimeSource.Monotonic
    val repository = InMemoryRepository()
    val executor = Executors.newCachedThreadPool()

    fun scheduleAfter(duration: Duration, func: () -> Unit) {
        val now = OffsetTime.now()
        repository.add(ScheduledTask(func, timeSource.markNow() + duration))
    }

    fun scheduleRecurring(period: Duration, func: () -> Unit) {
        repository.add(RecurringTask(func, timeSource.markNow() + period, period))
    }

    fun run() {
        while (true) {
            val tasksToRun = repository.pickDueExecution()
            for (task in tasksToRun) {
//                task.func()
//                task.completionHandler(this)
                executor.submit{
                    task.func()
                    task.completionHandler(this)
                }
            }
        }
    }
}