package org.schedo.retry

import java.time.Duration
import java.time.OffsetDateTime

sealed class RetryPolicy(val maxRetries: Int) {
    class FixedDelay(maxRetries: Int = 3, val delay: Duration = Duration.ofSeconds(10)) : RetryPolicy(maxRetries) {
        override fun getNextRetryTime(prevFail: OffsetDateTime?, currFail: OffsetDateTime): OffsetDateTime =
            currFail + delay
    }

    // TODO calculate using firstDelay * multiplier^count
    class ExpBackoff(maxRetries: Int = 3, val firstDelay: Duration = Duration.ofSeconds(2), val multiplier: Double = 2.0) : RetryPolicy(maxRetries) {
        override fun getNextRetryTime(prevFail: OffsetDateTime?, currFail: OffsetDateTime): OffsetDateTime =
            if (prevFail == null) {
                currFail + firstDelay
            } else {
                val lastInterval = Duration.between(prevFail, currFail)
                val nextDelay = Duration.ofMillis(
                    (lastInterval.toMillis() * multiplier).toLong()
                )
                currFail + nextDelay
            }
    }

    abstract fun getNextRetryTime(prevFail: OffsetDateTime?, currFail: OffsetDateTime): OffsetDateTime
}