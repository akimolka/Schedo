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
import com.cronutils.model.time.ExecutionTime
import org.schedo.server.SchedoServer
import org.schedo.waiter.Waiter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class Scheduler(
    private val taskManager: TaskManager,
    private val server: SchedoServer?,
    private val waiter: Waiter,
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
    private val dateTimeService: DateTimeService = DefaultDateTimeService(),
    private val cronParser: CronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ)),
    private val cronDescriptor: CronDescriptor = CronDescriptor.instance(Locale.UK),
) {

    @Volatile
    private var stopFlag = false
    private var executingCount: AtomicInteger = AtomicInteger(0)

    private fun run() {
        while (!stopFlag) {
            val tasks = taskManager.pickDueNow()
            executingCount.getAndAdd(tasks.size)
            tasks.forEach { (instanceID, task) ->
                executor.submit {
                    task.exec(instanceID, this.taskManager)

                    executingCount.getAndDecrement()
                    if (!waiter.isBusy(executingCount)) {
                        waiter.wakePoller()
                    }
                }
            }

            if (tasks.isEmpty()) {
                waiter.sleepPollingInterval()
            } else if (waiter.isBusy(executingCount)) {
                waiter.waitLightLoad(executingCount)
            }
        }
    }

    fun scheduleAt(name: String, moment: OffsetDateTime, func: () -> Unit) =
        scheduleAt(name, moment, null, func)

    fun scheduleAt(name: String, moment: OffsetDateTime, retryPolicy: RetryPolicy?, func: () -> Unit) {
        val now = dateTimeService.now()
        if (moment.isBefore(dateTimeService.now())) {
            logger.warn { "Task '$name' will be executed immediately. Scheduled time $moment is in the past (now = $now)" }
        }
        taskManager.schedule(object : OneTimeTask(TaskName(name), retryPolicy) {
            override fun run() = func()
        }, moment)
    }

    fun scheduleAfter(name: String, duration: TemporalAmount, func: () -> Unit) =
        scheduleAfter(name, duration, null, func)

    fun scheduleAfter(name: String, duration: TemporalAmount, retryPolicy: RetryPolicy?, func: () -> Unit) =
        scheduleAt(name, dateTimeService.now().plus(duration), retryPolicy, func)

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

    /**
     * Schedule recurring task [name] that runs with [recurringSchedule] and [retryPolicy].
     */
    private fun scheduleRecurring(name: String, recurringSchedule: RecurringSchedule,
                                  retryPolicy: RetryPolicy?, func: () -> Unit) =
        taskManager.schedule(object : RecurringTask(TaskName(name), recurringSchedule, retryPolicy) {
            override fun run() = func()
        }, recurringSchedule.nextExecution(dateTimeService.now()))

    fun start(join: Boolean = false) {
        logger.info{ "Scheduler started" }
        server?.run()
        val thread = thread {
            run()
        }
        if (join) {
            thread.join()
        }
    }

    fun stop() {
        logger.info{ "Scheduler received a request to stop" }
        stopFlag = true
        executor.shutdown()
    }
}