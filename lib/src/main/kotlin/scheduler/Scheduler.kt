package scheduler

import repository.InMemoryRepository
import repository.Repository
import task.RecurringTask
import task.ScheduledTask
import task.TaskManager
import task.TaskName
import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Scheduler(
    repository: Repository = InMemoryRepository(),
    private val executor: ExecutorService = Executors.newCachedThreadPool()
) {

    val taskManager: TaskManager = TaskManager(repository)

    @Volatile
    private var stopFlag = false;

    private fun run() {
        while (!stopFlag) {
            taskManager.pickDueNow().forEach { executor.submit { it.exec(this) } }
        }
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) {
        taskManager.schedule(object : ScheduledTask(TaskName(name)) {
            override fun run() {
                func()
            }

            override fun onCompleted(scheduler: Scheduler) {}
            override fun onFailed(e: Exception, scheduler: Scheduler) {}

        }, OffsetDateTime.now() + duration)

    }

    fun scheduleRecurring(name: String, period: TemporalAmount, func: () -> Unit) {
        taskManager.schedule(object : RecurringTask(TaskName(name), period) {
            override fun run() {
                func()
            }
        }, OffsetDateTime.now() + period)
    }

    fun start() {
        thread {
            run()
        }
    }

    fun stop() {
        stopFlag = true
        executor.shutdown()
    }
}