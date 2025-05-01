package org.schedo.task

import org.schedo.repository.TaskResult
import org.schedo.scheduler.Scheduler
import java.time.Duration
import java.util.*
import kotlin.system.measureTimeMillis

@JvmInline
value class TaskName(val value: String)

/**
 * General description of the task without reference to its execution time.
 * Contains name, payload and handlers.
 * Each time a task is scheduled, its instance is created,
 * namely a unique TaskInstanceID is generated.
 */
abstract class Task(
    val name: TaskName,
) {
    /**
     * Payload
     */
    abstract fun run()

    /**
     * Called if task execution is successful
     */
    abstract fun onCompleted(scheduler: Scheduler)

    /**
     * Called if task execution throws an exception
     */
    abstract fun onFailed(e: Exception, scheduler: Scheduler)

    fun exec(id: TaskInstanceID, scheduler: Scheduler) = try {
        scheduler.taskManager.updateTaskStatusStart(id)
        val timeSpending = measureTimeMillis {
            run()
        }
        scheduler.taskManager.updateTaskStatusFinish(id, TaskResult.Success(Duration.ofMillis(timeSpending)))
        onCompleted(scheduler)
    } catch (e: Exception) {
        scheduler.taskManager.updateTaskStatusFinish(id, TaskResult.Failed(e))
        onFailed(e, scheduler)
    }
}

/**
 * TaskInstanceID is unique among TaskInstanceIDs of all instances of all tasks
 */
@JvmInline
value class TaskInstanceID(val value: UUID)

data class TaskInstanceName(val id: TaskInstanceID, val name: TaskName)

/**
 * TaskInstance represents a single Task execution.
 * An instance corresponds to exactly one row in SchedoTasks and SchedoStatus.
 */
data class TaskInstance(val id: TaskInstanceID, val task: Task)