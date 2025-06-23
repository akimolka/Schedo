package org.schedo.retry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.math.pow

class RetryPolicyTest {

 @Nested
 inner class FixedDelayTests {
  private val delay = Duration.ofSeconds(7)
  private val policy = RetryPolicy.FixedDelay(maxRetries = 3u, delay = delay)

  @Test
  fun `returns delay when failedCount is less than maxRetries`() {
   assertEquals(delay, policy.getNextDelay(0u))
   assertEquals(delay, policy.getNextDelay(1u))
   assertEquals(delay, policy.getNextDelay(2u))
  }

  @Test
  fun `returns null when failedCount equals or exceeds maxRetries`() {
   assertNull(policy.getNextDelay(3u))
   assertNull(policy.getNextDelay(4u))
  }
 }

 @Nested
 inner class ExpBackoffTests {
  private val firstDelay = Duration.ofSeconds(2)
  private val multiplier = 2.0
  private val policy = RetryPolicy.ExpBackoff(maxRetries = 3u, firstDelay = firstDelay, multiplier = multiplier)

  @Test
  fun `returns exponential delay when failedCount is less than maxRetries`() {
   val expected0 = Duration.ofMillis((2000 * 2.0.pow(0)).toLong()) // 2000 ms
   val expected1 = Duration.ofMillis((2000 * 2.0.pow(1)).toLong()) // 4000 ms
   val expected2 = Duration.ofMillis((2000 * 2.0.pow(2)).toLong()) // 8000 ms

   assertEquals(expected0, policy.getNextDelay(0u))
   assertEquals(expected1, policy.getNextDelay(1u))
   assertEquals(expected2, policy.getNextDelay(2u))
  }

  @Test
  fun `returns null when failedCount equals or exceeds maxRetries`() {
   assertNull(policy.getNextDelay(3u))
   assertNull(policy.getNextDelay(4u))
  }
 }

 @Nested
 inner class CornerCaseTests {

  @Test
  fun `FixedDelay with maxRetries = 0 always returns null`() {
   val policy = RetryPolicy.FixedDelay(maxRetries = 0u, delay = Duration.ofSeconds(5))

   assertNull(policy.getNextDelay(0u))
   assertNull(policy.getNextDelay(1u))
   assertNull(policy.getNextDelay(100u))
  }

  @Test
  fun `ExpBackoff with maxRetries = 0 always returns null`() {
   val policy = RetryPolicy.ExpBackoff(maxRetries = 0u, firstDelay = Duration.ofSeconds(2),
    multiplier = 1.5)

   assertNull(policy.getNextDelay(0u))
   assertNull(policy.getNextDelay(1u))
   assertNull(policy.getNextDelay(100u))
  }

  @Test
  fun `FixedDelay with delay = 0 returns zero if under retry limit else null`() {
   val zeroDelay = Duration.ZERO
   val policy = RetryPolicy.FixedDelay(maxRetries = 5u, delay = zeroDelay)

   assertEquals(zeroDelay, policy.getNextDelay(0u))
   assertEquals(zeroDelay, policy.getNextDelay(1u))
   assertEquals(zeroDelay, policy.getNextDelay(4u))
   assertNull(policy.getNextDelay(5u)) // after maxRetries
  }
 }

}
