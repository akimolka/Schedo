package org.schedo.util

import com.cronutils.model.time.ExecutionTime
import java.time.OffsetDateTime
import java.time.ZoneId

interface DateTimeService {
    fun now(): OffsetDateTime
}

class DefaultDateTimeService : DateTimeService{
    override fun now(): OffsetDateTime =
        OffsetDateTime.now()
}

fun nextExecution(executionTime: ExecutionTime, nowOffset: OffsetDateTime): OffsetDateTime {
    val zone: ZoneId = ZoneId.systemDefault()
    val nowZoned = nowOffset.toInstant().atZone(zone) // TODO
    val nextZoned = executionTime.nextExecution(nowZoned).get()
    return nextZoned.toOffsetDateTime()
}