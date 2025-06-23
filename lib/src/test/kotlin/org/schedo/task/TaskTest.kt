package org.schedo.task

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.schedo.manager.ITaskManager
import org.schedo.manager.TaskResult
import org.schedo.retry.RetryPolicy
import org.schedo.util.DateTimeService
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TaskTest {

 private lateinit var taskManager: ITaskManager
 private lateinit var dateTimeService: DateTimeService
 private val taskName = TaskName("demo-task")
 private val instanceId = TaskInstanceID("123")

 @BeforeEach
 fun setup() {
  dateTimeService = mockk()
  taskManager = mockk(relaxed = true) {
   every { dateTimeService } returns this@TaskTest.dateTimeService
  }
 }

 @Test
 fun `exec - should call updateTaskStatusStarted and Finished with Success`() {
  val task = object : Task(taskName) {
   override fun run() { }
  }

  task.exec(instanceId, taskManager)

  verifySequence {
   taskManager.updateTaskStatusStarted(instanceId)
   taskManager.updateTaskStatusFinished(taskName, instanceId, match { it is TaskResult.Success })
  }
 }

 @Test
 fun `exec - should retry if exception and retryPolicy allows`() {
  val retryPolicy = RetryPolicy.FixedDelay(maxRetries = 3u, delay = Duration.ofSeconds(1))
  val now = OffsetDateTime.now()

  val task = object : Task(taskName, retryPolicy) {
   override fun run() {
    throw RuntimeException("fail")
   }
  }

  every { taskManager.failedCount(taskName, 3u) } returns 0u
  every { dateTimeService.now() } returns now

  task.exec(instanceId, taskManager)

  verifyOrder {
   taskManager.updateTaskStatusStarted(instanceId)
   taskManager.schedule(taskName, now.plusSeconds(1), isRetry = true, any())
   taskManager.updateTaskStatusFinished(taskName, instanceId, match { it is TaskResult.Failed })
  }
 }

 @Test
 fun `exec - should not retry if retryPolicy exhausted`() {
  val retryPolicy = RetryPolicy.FixedDelay(maxRetries = 1u, delay = Duration.ofSeconds(1))
  val task = object : Task(taskName, retryPolicy) {
   override fun run() {
    throw RuntimeException("fail")
   }
  }

  var failureCalled = false
  task.failureHandlers = listOf { failureCalled = true }

  every { taskManager.failedCount(taskName, 1u) } returns 1u

  task.exec(instanceId, taskManager)

  verify(exactly = 0) { taskManager.schedule(any(), any(), any()) }
  assertTrue(failureCalled)
 }

 @Test
 fun `exec - should invoke successHandlers`() {
  var successCalled = false
  var exceptionCalled = false
  val task = object : Task(taskName) {
   override fun run() {}
  }
  task.successHandlers = listOf { successCalled = true }
  task.exceptionHandlers = listOf { _, _ -> exceptionCalled = true }

  task.exec(instanceId, taskManager)

  assertTrue(successCalled)
  assertFalse(exceptionCalled)
 }

 @Test
 fun `exec - should invoke exceptionHandlers`() {
  var successCalled = false
  var exceptionCalled = false
  val task = object : Task(taskName) {
   override fun run() { throw RuntimeException("fail") }
  }
  task.successHandlers = listOf { successCalled = true }
  task.exceptionHandlers = listOf { _, _ -> exceptionCalled = true }

  every { taskManager.failedCount(taskName, any()) } returns UInt.MAX_VALUE

  task.exec(instanceId, taskManager)

  assertFalse(successCalled)
  assertTrue(exceptionCalled)
 }
}
