package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.manager.TaskResult
import org.schedo.retry.RetryPolicy
import java.time.Duration
import kotlin.system.measureTimeMillis

class Sequence(
    val name: String,
    val head: TaskName,
    val dependencies: List<Task> = emptyList(),
)

class SequenceBuilder(
    val name: String,
    val tail: Task, // not completely formed and not added to dependencies
    val head: TaskName = TaskName(name + tail.name.value),
    val dependencies: List<Task> = emptyList(),
) {
    fun build(): Sequence {
        val last = object : Task(TaskName(name + tail.name.value), tail.retryPolicy) {
            override fun run() = tail.run()
            override fun onCompleted(taskManager: TaskManager) {
                tail.onCompleted(taskManager)
            }
            override fun onFailed(e: Exception, taskManager: TaskManager) {
                tail.onFailed(e, taskManager)
            }
        }

        return Sequence(name, head, dependencies + last)
    }

    fun andThen(next: Task): SequenceBuilder {
        val builder = this

        val last = object : Task(TaskName(name + tail.name.value), tail.retryPolicy) {
            override fun run() = tail.run()
            override fun onCompleted(taskManager: TaskManager) {
                tail.onCompleted(taskManager)
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(TaskName(builder.name + next.name.value), now)
            }
            override fun onFailed(e: Exception, taskManager: TaskManager) {
                tail.onFailed(e, taskManager)
            }
        }

        return SequenceBuilder(name, next, head, dependencies + last)
    }

    fun orElse(next: Task): SequenceBuilder {
        val builder = this

        val last = object : Task(TaskName(name + tail.name.value), tail.retryPolicy) {
            override fun run() = tail.run()
            override fun onCompleted(taskManager: TaskManager) {
                tail.onCompleted(taskManager)
            }
            override fun onFailed(e: Exception, taskManager: TaskManager) {
                tail.onFailed(e, taskManager)
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(TaskName(builder.name + next.name.value), now)
            }
        }

        return SequenceBuilder(name, next, head,dependencies + last)
    }
}

class TaskBuilder(val name: String, val func: () -> Unit) {
    var retryPolicy: RetryPolicy? = null
    var onCompletedHandler: (TaskManager) -> Unit = {_ -> }
    var onFailedHandler: (e: Exception, taskManager: TaskManager) -> Unit = { _, _ -> }

    fun retryPolicy(retryPolicy: RetryPolicy): TaskBuilder {
        this.retryPolicy = retryPolicy
        return this
    }

    fun onCompletedHandler(onCompletedHandler: (TaskManager) -> Unit): TaskBuilder {
        this.onCompletedHandler = onCompletedHandler
        return this
    }

    fun onFailedHandler(onFailedHandler: (Exception, taskManager: TaskManager) -> Unit): TaskBuilder {
        this.onFailedHandler = onFailedHandler
        return this
    }

    fun build(): Task {
        return object : Task(TaskName(name), retryPolicy) {
            override fun run() =  func()
            override fun onCompleted(taskManager: TaskManager) = onCompletedHandler(taskManager)
            override fun onFailed(e: Exception, taskManager: TaskManager) {
                onFailedHandler(e, taskManager)
                super.onFailed(e, taskManager) // handle retry
            }
        }
    }
}