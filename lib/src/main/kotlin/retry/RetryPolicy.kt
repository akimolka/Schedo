package retry

import java.time.Duration
import java.time.OffsetDateTime

sealed class RetryPolicy(val maxRetries: Int) {
    //data object DoNotRetry : RetryPolicy
    class FixedDelay(maxRetries: Int = 3, val delay: Duration = Duration.ofSeconds(10)) : RetryPolicy(maxRetries)
    class ExpBackoff(maxRetries: Int = 3, val firstDelay: Duration = Duration.ofSeconds(2), val multiplier: Double = 2.0) : RetryPolicy(maxRetries)
}

fun getNextRetryTime(prevFail: OffsetDateTime?, currFail: OffsetDateTime, retryPolicy: RetryPolicy): OffsetDateTime {
    return when (retryPolicy) {
        is RetryPolicy.FixedDelay -> currFail + retryPolicy.delay
        is RetryPolicy.ExpBackoff -> {
            if (prevFail == null) {
                currFail + retryPolicy.firstDelay
            } else {
                val lastInterval = Duration.between(prevFail, currFail)
                val nextDelay = Duration.ofMillis(
                    (lastInterval.toMillis() * retryPolicy.multiplier).toLong()
                )
                currFail + nextDelay
            }
        }
    }
}