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