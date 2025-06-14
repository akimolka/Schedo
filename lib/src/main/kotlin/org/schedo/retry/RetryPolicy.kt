package org.schedo.retry

import java.time.Duration
import kotlin.math.pow

/**
 * @param maxRetries limits the number of retries after the first execution. So that
 * [maxRetries] = 0 results in no retries at all, it is equivalent to retryPolicy = null.
 * If [maxRetries] is set to one, task will be rescheduled once if it fails.
 */
sealed class RetryPolicy(val maxRetries: UInt) {
    class FixedDelay(
        maxRetries: UInt = 3u,
        private val delay: Duration = Duration.ofSeconds(10)
    ) : RetryPolicy(maxRetries) {
        override fun getNextDelay(failedCount: UInt): Duration? =
            if (failedCount >= maxRetries) null else delay
    }

    class ExpBackoff(
        maxRetries: UInt = 3u,
        private val firstDelay: Duration = Duration.ofSeconds(2),
        private val multiplier: Double = 2.0
    ) : RetryPolicy(maxRetries) {
        override fun getNextDelay(failedCount: UInt): Duration? =
            if (failedCount >= maxRetries) {
                null
            } else {
                val factor = multiplier.pow(failedCount.toDouble())
                val nextMillis = (firstDelay.toMillis().toDouble() * factor).toLong()
                Duration.ofMillis(nextMillis)
            }
    }

    /**
     * @param failedCount the number of falls excluding current fall
     */
    abstract fun getNextDelay(failedCount: UInt): Duration?
}