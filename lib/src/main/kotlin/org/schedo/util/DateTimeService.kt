package org.schedo.util

import java.time.OffsetDateTime

interface DateTimeService {
    fun now(): OffsetDateTime
}

class DefaultDateTimeService : DateTimeService{
    override fun now(): OffsetDateTime =
        OffsetDateTime.now()
}