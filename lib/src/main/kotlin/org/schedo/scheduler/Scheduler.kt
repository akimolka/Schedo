package org.schedo.scheduler

import java.time.temporal.TemporalAmount
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy
import org.schedo.task.*
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import java.time.OffsetDateTime

import com.cronutils.model.CronType.QUARTZ
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.definition.CronDefinition
import com.cronutils.model.time.ExecutionTime
import org.schedo.util.nextExecution
import java.util.*

private val logger = KotlinLogging.logger {}

class Scheduler(
    val taskManager: TaskManager,
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
    val dateTimeService: DateTimeService = DefaultDateTimeService(),
    private val cronParser: CronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ)),
    private val cronDescriptor: CronDescriptor = CronDescriptor.instance(Locale.UK),
) {

    @Volatile
    private var stopFlag = false;

    private fun run() {
        while (!stopFlag) {
            taskManager.pickDueNow().forEach { (instanceID, task) ->
                executor.submit { task.exec(instanceID, this.taskManager) }
            }
            Thread.yield()
        }
    }

    fun scheduleAt(name: String, moment: OffsetDateTime, func: () -> Unit) =
        scheduleAt(name, moment, null, func)

    fun scheduleAt(name: String, moment: OffsetDateTime, retryPolicy: RetryPolicy?, func: () -> Unit) {
        val now = dateTimeService.now()
        if (moment.isBefore(dateTimeService.now())) {
            logger.warn { "Cannot schedule task '$name' at $moment: time is in the past (now = $now)" }
        } else {
            taskManager.schedule(object : OneTimeTask(TaskName(name), retryPolicy) {
                override fun run() = func()
            }, moment)
        }
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) =
        scheduleAfter(name, duration, null, func)

    fun scheduleAfter(name: String, duration: TemporalAmount, retryPolicy: RetryPolicy?, func: () -> Unit) =
        scheduleAt(name, dateTimeService.now().plus(duration), retryPolicy, func)

    fun scheduleRecurring(name: String, duration: TemporalAmount, func: () -> Unit) =
        scheduleRecurring(name, duration, null, func)

    fun scheduleRecurring(name: String, period: TemporalAmount, retryPolicy: RetryPolicy?, func: () -> Unit) {
        taskManager.schedule(object : RecurringTask(TaskName(name), period, retryPolicy) {
            override fun run() = func()
        }, dateTimeService.now() + period)
    }

    fun scheduleRecurringCron(name: String, cron: String, func: () -> Unit) =
        scheduleRecurringCron(name, cron, null, func)

    fun scheduleRecurringCron(name: String, cronExpr: String, retryPolicy: RetryPolicy?, func: () -> Unit) {
        val cron = cronParser.parse(cronExpr)
        logger.info { "Task $name with schedule ${cronDescriptor.describe(cron)} has been scheduled" }
        val executionTime = ExecutionTime.forCron(cron)
        val next = nextExecution(executionTime, dateTimeService.now())
        taskManager.schedule(object : RecurringCronTask(TaskName(name), executionTime, retryPolicy) {
            override fun run() = func()
        }, next)
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