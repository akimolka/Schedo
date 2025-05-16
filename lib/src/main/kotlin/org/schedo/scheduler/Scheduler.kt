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

    fun scheduleAt(task: CustomTask, moment: OffsetDateTime) {
        val now = dateTimeService.now()
        if (moment.isBefore(dateTimeService.now())) {
            logger.warn { "Cannot schedule task '${task.name}' at $moment: time is in the past (now = $now)" }
        } else {
            for (dep in task.dependencies) {
                taskManager.register(dep)
            }
            taskManager.schedule(object : OneTimeTask(task.name, task.retryPolicy) {
                override fun run() = task.run()
                override fun onCompleted(taskManager: TaskManager) = task.onCompleted(taskManager)
                override fun onFailed(e: Exception, taskManager: TaskManager) = task.onFailed(e, taskManager)
            }, moment)
        }
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) =
        scheduleAfter(name, duration, null, func)

    fun scheduleAfter(name: String, duration: TemporalAmount, retryPolicy: RetryPolicy?, func: () -> Unit) =
        scheduleAt(name, dateTimeService.now().plus(duration), retryPolicy, func)

    fun scheduleAfter(task: CustomTask, duration: TemporalAmount) =
        scheduleAt(task, dateTimeService.now().plus(duration))

    /**
     * Schedule recurring task [name] that repeats once in [period] without retry strategy.
     */
    fun scheduleRecurring(name: String, period: TemporalAmount, func: () -> Unit) =
        scheduleRecurring(name, period, null, func)

    /**
     * Schedule recurring task [name] that repeats once in [period] with [retryPolicy] strategy.
     */
    fun scheduleRecurring(name: String, period: TemporalAmount, retryPolicy: RetryPolicy?, func: () -> Unit) {
        val recurringSchedule = FixedDelaySchedule(period)
        scheduleRecurring(name, recurringSchedule, retryPolicy, func)
    }

    fun scheduleRecurring(task: CustomTask, period: TemporalAmount) {
        val recurringSchedule = FixedDelaySchedule(period)
        scheduleRecurring(task, recurringSchedule)
    }

    /**
     * Schedule recurring task [name] that is executed at moments described by [cron] expression.
     */
    fun scheduleRecurring(name: String, cron: String, func: () -> Unit) =
        scheduleRecurring(name, cron, null, func)

    /**
     * Schedule recurring task [name] that is executed at moments described by [cron] expression
     * with [retryPolicy] strategy.
     */
    fun scheduleRecurring(name: String, cronExpr: String, retryPolicy: RetryPolicy?, func: () -> Unit) {
        val cron = cronParser.parse(cronExpr)
        logger.info { "Task $name with schedule ${cronDescriptor.describe(cron)} has been scheduled" }
        val recurringSchedule = CronSchedule(ExecutionTime.forCron(cron))
        scheduleRecurring(name, recurringSchedule, retryPolicy, func)
    }

    fun scheduleRecurring(task: CustomTask, cronExpr: String) {
        val cron = cronParser.parse(cronExpr)
        logger.info { "Task ${task.name} with schedule ${cronDescriptor.describe(cron)} has been scheduled" }
        val recurringSchedule = CronSchedule(ExecutionTime.forCron(cron))
        scheduleRecurring(task, recurringSchedule)
    }

    /**
     * Schedule recurring task [name] that runs with [recurringSchedule] and [retryPolicy].
     */
    private fun scheduleRecurring(name: String, recurringSchedule: RecurringSchedule,
                                  retryPolicy: RetryPolicy?, func: () -> Unit) =
        taskManager.schedule(object : RecurringTask(TaskName(name), recurringSchedule, retryPolicy) {
            override fun run() = func()
        }, recurringSchedule.nextExecution(dateTimeService.now()))

    private fun scheduleRecurring(task: CustomTask, recurringSchedule: RecurringSchedule) {
        for (dep in task.dependencies) {
            taskManager.register(dep)
        }
        taskManager.schedule(object : RecurringTask(task.name, recurringSchedule, task.retryPolicy) {
            override fun run() = task.run()
            override fun onCompleted(taskManager: TaskManager) {
                task.onCompleted(taskManager)
                super.onCompleted(taskManager) // reschedule
            }
            override fun onFailed(e: Exception, taskManager: TaskManager) = task.onFailed(e, taskManager)
        }, recurringSchedule.nextExecution(dateTimeService.now()))
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