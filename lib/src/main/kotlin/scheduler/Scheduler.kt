package scheduler
import repository.InMemoryRepository
import repository.Repository
import task.RecurringTask
import task.ScheduledTask
import java.time.OffsetTime
import java.time.temporal.TemporalAmount
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Scheduler(val repository: Repository = InMemoryRepository(), val executor: ExecutorService = Executors.newCachedThreadPool()) {
    @Volatile
    private var stopFlag = false;

    fun scheduleAfter(duration: TemporalAmount, func: () -> Unit) {
        val now = OffsetTime.now()
        repository.add(ScheduledTask(func, now.plus(duration)))
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) {
        val now = OffsetTime.now()
        repository.add(ScheduledTask(func, now.plus(duration), name))
    }

    fun scheduleRecurring(period: TemporalAmount, func: () -> Unit) {
        val now = OffsetTime.now()
        repository.add(RecurringTask(func, now.plus(period), period))
    }

    fun scheduleRecurring(name: String, period: TemporalAmount, func: () -> Unit) {
        val now = OffsetTime.now()
        repository.add(RecurringTask(func, now.plus(period), period, name))
    }

    fun run() {
        while (true) {
            val tasksToRun = repository.pickDue(OffsetTime.now())
            for (task in tasksToRun) {
                executor.submit{
                    task.func()
                    task.completionHandler(this)
                }
            }
            if (stopFlag) {
                break
            }
            Thread.yield()
        }
    }

    fun start() {
        thread {
            run()
        }
    }

    fun stop() {
        stopFlag = true;
    }
}