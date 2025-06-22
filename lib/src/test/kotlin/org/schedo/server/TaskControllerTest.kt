package org.schedo.server

import io.mockk.*
import org.junit.jupiter.api.Test
import org.schedo.manager.ITaskManager
import org.schedo.repository.*
import org.schedo.task.*
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskControllerTest {

 private val tasksRepository = mockk<TasksRepository>()
 private val statusRepository = mockk<StatusRepository>()
 private val executionsRepository = mockk<ExecutionsRepository>()
 private val tm = mockk<TransactionManager>()
 private val taskManager = mockk<ITaskManager>(relaxed = true)

 private val controller = TaskController(
  tasksRepository,
  statusRepository,
  executionsRepository,
  tm,
  taskManager
 )

 private val taskNameA = TaskName("task A")
 private val taskNameB = TaskName("task B")
 private val instance1 = TaskInstanceID("1")
 private val instance2 = TaskInstanceID("2")
 private val instance3 = TaskInstanceID("3")
 private val instance4 = TaskInstanceID("4")
 private val now = OffsetDateTime.now()

 @Test
 fun `countScheduledTasks delegates to tasksRepository`() {
  every { tm.transaction<Int>(any()) } answers {
   val block = firstArg<() -> Int>()
   block()
  }
  every { tasksRepository.countTaskInstancesDue(any()) } returns 3

  val result = controller.countScheduledTasks(now)

  assertEquals(3, result)
 }

 @Test
 fun `scheduledTasks returns sorted list from tasksRepository`() {
  val task1 = ScheduledTaskInstance(instance1, taskNameA, now.plusSeconds(10))
  val task2 = ScheduledTaskInstance(instance2, taskNameB, now)

  every { tm.transaction<List<ScheduledTaskInstance>>(any()) } answers {
   val block = firstArg<() -> List<ScheduledTaskInstance>>()
   block()
  }
  every { tasksRepository.listTaskInstancesDue(any()) } returns listOf(task1, task2)

  val result = controller.scheduledTasks()

  assertEquals(listOf(task2, task1), result)
 }

 @Test
 fun `failedTasks returns only tasks that failed at least once`() {
  val failedTime = now.minusMinutes(10)
  val finished = listOf(
   FinishedTask(instance1, taskNameA, Status.FAILED, failedTime, AdditionalInfo("fail 1")),
   FinishedTask(instance2, taskNameA, Status.COMPLETED, now.minusMinutes(1), null) ,
   FinishedTask(instance3, taskNameB, Status.COMPLETED, now.minusMinutes(10), null),
   FinishedTask(instance4, taskNameB, Status.CANCELLED, now.minusMinutes(1), AdditionalInfo("cancel 4"))
  )
  every { tm.transaction<List<FinishedTask>>(any()) } answers {
   val block = firstArg<() -> List<FinishedTask>>()
   block()
  }
  every { statusRepository.finishedTasks() } returns finished

  val result = controller.failedTasks()

  assertEquals(1, result.size)
  assertEquals("fail 1", result.first().errorMessage)
  assertEquals(failedTime, result.first().lastFailure)
  assertEquals(true, result.first().recovered)
 }

 @Test
 fun `failedTasks sets recovered if after failure was success`() {
  val taskNameC = TaskName("C")
  val finished = listOf(
   // A: failed, completed, failed - recovered = false
   FinishedTask(instance1, taskNameA, Status.COMPLETED, now.minusMinutes(5), null),
   FinishedTask(instance2, taskNameA, Status.FAILED, now.minusMinutes(1), null) ,
   FinishedTask(instance3, taskNameA, Status.FAILED, now.minusMinutes(10), null),
   // B: failed, cancelled - recovered = false
   FinishedTask(instance4, taskNameB, Status.FAILED, now.minusMinutes(5), null),
   FinishedTask(TaskInstanceID("5"), taskNameB, Status.CANCELLED, now.minusMinutes(1), null),
   // C: failed, cancelled, completed - recovered = true
   FinishedTask(TaskInstanceID("6"), taskNameC, Status.FAILED, now.minusMinutes(10), null),
   FinishedTask(TaskInstanceID("7"), taskNameC, Status.COMPLETED, now.minusMinutes(1), null),
   FinishedTask(TaskInstanceID("8"), taskNameC, Status.CANCELLED, now.minusMinutes(5), null),
  )
  val expected = listOf(Pair(taskNameA, false), Pair(taskNameB, false), Pair(taskNameC, true))

  every { tm.transaction<List<FinishedTask>>(any()) } answers {
   val block = firstArg<() -> List<FinishedTask>>()
   block()
  }
  every { statusRepository.finishedTasks() } returns finished

  val result = controller.failedTasks()
  val actual = result.map { it.name to it.recovered }
  assertEquals(expected, actual)
 }

 @Test
 fun `failedTasks returns sorted list`() {
  val finished = listOf(
   FinishedTask(instance1, taskNameA, Status.FAILED, now.minusMinutes(50), null),
   FinishedTask(instance2, taskNameB, Status.FAILED, now.minusMinutes(1), null) ,
  )
  val expected = listOf(taskNameB, taskNameA)

  every { tm.transaction<List<FinishedTask>>(any()) } answers {
   val block = firstArg<() -> List<FinishedTask>>()
   block()
  }
  every { statusRepository.finishedTasks() } returns finished

  val result = controller.failedTasks()
  val actual = result.map { it.name }
  assertEquals(expected, actual)
 }

 @Test
 fun `taskHistory returns DetailedTaskInfo with sorted history`() {
  val history = listOf(StatusEntry(instance1, Status.SCHEDULED, now.minusMinutes(4), now.minusMinutes(5)),
                       StatusEntry(instance2, Status.COMPLETED, now.minusMinutes(9), now.minusMinutes(10)))
  val statusAndCancelled = TaskStatus.RUNNING to true

  every { tm.transaction<DetailedTaskInfo>(any()) } answers {
   val block = firstArg<() -> DetailedTaskInfo>()
   block()
  }
  every { executionsRepository.getStatusAndCancelled(taskNameA) } returns statusAndCancelled
  every { statusRepository.taskHistory(taskNameA) } returns history

  val result = controller.taskHistory(taskNameA)

  assertEquals(history.sortedByDescending { it.createdAt }, result.history)
  assertEquals(TaskStatus.RUNNING, result.executionStatus)
  assertEquals(true, result.isCancelled)
 }

 @Test
 fun `tasks returns aggregated info when taskName is null`() {
  val history = listOf(
   // A: failed, cancelled, scheduled -> 0 completed, 1 failed
   // B: completed, started -> 1 completed, 0 failed
   taskNameA to StatusEntry(instance1, Status.FAILED, now, now),
   taskNameB to StatusEntry(instance2, Status.COMPLETED, now, now),
   taskNameA to StatusEntry(instance3, Status.CANCELLED, now, now),
   taskNameA to StatusEntry(instance4, Status.SCHEDULED, now, now),
   taskNameB to StatusEntry(TaskInstanceID("5"), Status.STARTED, now, now),
  )
  val expected = listOf(
   AggrTaskInfo(taskNameA, 0, 1, null),
   AggrTaskInfo(taskNameB, 1, 0, null)
  )

  every { tm.transaction<List<Pair<TaskName, StatusEntry>>>(any()) } answers {
   val block = firstArg<() -> List<Pair<TaskName, StatusEntry>>>()
   block()
  }
  every { statusRepository.history(any(), any()) } returns history

  val result = controller.tasks(now.minusDays(1), now.plusDays(1), null, null)

  assertEquals(expected.toSet(), result.toSet())
 }

 @Test
 fun `resumeTask delegates to taskManager`() {
  every { taskManager.resume(taskNameA) } returns true

  assertTrue(controller.resumeTask(taskNameA))
 }

 @Test
 fun `forceResumeTask delegates to taskManager`() {
  every { taskManager.forceResume(taskNameA) } just Runs

  controller.forceResumeTask(taskNameA)

  verify { taskManager.forceResume(taskNameA) }
 }

 @Test
 fun `cancelTask calls executionsRepository and returns result`() {
  every { taskManager.dateTimeService.now() } returns now
  every { tm.transaction<Boolean>(any()) } answers {
   val block = firstArg<() -> Boolean>()
   block()
  }
  every { executionsRepository.cancel(taskNameA, now) } returns true

  assertTrue(controller.cancelTask(taskNameA))
 }
}
