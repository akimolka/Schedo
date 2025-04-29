package scheduler

import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import io.github.oshai.kotlinlogging.KotlinLogging
import manager.TaskManager
import repository.Repository
import repository.RepositoryType
import task.*

private val logger = KotlinLogging.logger {}

class Scheduler(
    private val repository: Repository = Repository(RepositoryType.InMemory),
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
) {

    val taskManager: TaskManager = TaskManager(repository)

    @Volatile
    private var stopFlag = false;

    private fun run() {
        while (!stopFlag) {
            taskManager.pickDueNow().forEach { executor.submit { it.exec(this) } }
            Thread.yield()
        }
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) {
        taskManager.schedule(object : OneTimeTask(TaskName(name)) {
            override fun run() {
                func()
            }
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
        logger.info{ "Scheduler started" }
        thread {
            run()
        }
    }

    fun stop() {
        logger.info{ "Scheduler received a request to stop" }
        stopFlag = true
        executor.shutdown()
    }
}