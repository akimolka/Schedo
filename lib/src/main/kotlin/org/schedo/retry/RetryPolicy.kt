package org.schedo.retry

import java.time.Duration
import kotlin.math.pow

/**
 * @param maxRetries limits the number of retries after the first execution. So that
 * [maxRetries] = 0 results in no retries at all, it is equivalent to retryPolicy = null.
 */
sealed class RetryPolicy(val maxRetries: Int) {
    class FixedDelay(maxRetries: Int = 3, val delay: Duration = Duration.ofSeconds(10)) : RetryPolicy(maxRetries) {
        override fun getNextDelay(failedCount: Int): Duration? =
            if (failedCount >= maxRetries) null else delay
    }

    class ExpBackoff(maxRetries: Int = 3, val firstDelay: Duration = Duration.ofSeconds(2), val multiplier: Double = 2.0) : RetryPolicy(maxRetries) {
        override fun getNextDelay(failedCount: Int): Duration? =
            if (failedCount >= maxRetries) null
            else Duration.ofMillis((firstDelay.toMillis() * multiplier.pow(failedCount)).toLong())
    }

    /**
     * @param failedCount the number of falls excluding current fall
     */
    abstract fun getNextDelay(failedCount: Int): Duration?
}