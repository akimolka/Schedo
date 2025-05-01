package org.schedo.scheduler

import java.time.temporal.TemporalAmount
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.manager.TaskManager
import org.schedo.task.*
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService

private val logger = KotlinLogging.logger {}

class Scheduler(
    val taskManager: TaskManager = TaskManager(),
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
    val dateTimeService: DateTimeService = DefaultDateTimeService(),
) {

    @Volatile
    private var stopFlag = false;

    private fun run() {
        while (!stopFlag) {
            taskManager.pickDueNow().forEach { (instanceID, task) ->
                executor.submit { task.exec(instanceID, this) }
            }
            Thread.yield()
        }
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) {
        taskManager.schedule(object : OneTimeTask(TaskName(name)) {
            override fun run() {
                func()
            }
        }, dateTimeService.now() + duration)

    }

    fun scheduleRecurring(name: String, period: TemporalAmount, func: () -> Unit) {
        taskManager.schedule(object : RecurringTask(TaskName(name), period) {
            override fun run() {
                func()
            }
        }, dateTimeService.now() + period)
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