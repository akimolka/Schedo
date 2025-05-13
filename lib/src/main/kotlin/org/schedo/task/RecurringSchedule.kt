package org.schedo.task

import com.cronutils.model.time.ExecutionTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAmount

interface RecurringSchedule {
    fun nextExecution(moment: OffsetDateTime): OffsetDateTime
}

class FixedDelaySchedule(private val period: TemporalAmount) : RecurringSchedule {
    // TODO Not clear that it calculates delay starting from the given time
    override fun nextExecution(moment: OffsetDateTime) =
        moment + period
}

class CronSchedule(
    private val executionTime: ExecutionTime,
    private val zone: ZoneId = ZoneId.systemDefault()
) : RecurringSchedule {
    override fun nextExecution(moment: OffsetDateTime): OffsetDateTime {
        val nowZoned = moment.toInstant().atZone(zone)
        val nextZoned = executionTime.nextExecution(nowZoned).get()
        return nextZoned.toOffsetDateTime()
    }
}