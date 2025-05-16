package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.retry.RetryPolicy

abstract class CustomTask (
    name: TaskName,
    val retryPolicy: RetryPolicy? = null,
    val dependencies: List<CustomTask> = emptyList(),
) : Task(name, retryPolicy) {
    fun andThen(next: CustomTask): CustomTask {
        val parent = this
        val composedName = TaskName("${name.value}_andThen_${next.name.value}")

        return object : CustomTask(composedName, retryPolicy, dependencies + next) {
            override fun run() = parent.run()

            override fun onCompleted(taskManager: TaskManager) {
                parent.onCompleted(taskManager)
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(next.name, now)
            }

            override fun onFailed(e: Exception, taskManager: TaskManager) {
                parent.onFailed(e, taskManager)
                super.onFailed(e, taskManager) // handle retry
            }
        }
    }

    fun orElse(next: CustomTask): CustomTask {
        val parent = this
        val composedName = TaskName("${name.value}_orElse_${next.name.value}")

        return object : CustomTask(composedName, retryPolicy, dependencies + next) {
            override fun run() = parent.run()

            override fun onCompleted(taskManager: TaskManager) {
                parent.onCompleted(taskManager)
            }

            override fun onFailed(e: Exception, taskManager: TaskManager) {
                parent.onFailed(e, taskManager)
                // Note: no attempt of retry
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(next.name, now)
            }
        }
    }

    fun fold(success: CustomTask, failure: CustomTask): CustomTask {
        val parent = this
        val composedName = TaskName("${name.value}_fold_${success.name.value}_${failure.name.value}")

        return object : CustomTask(composedName, retryPolicy, dependencies + success + failure) {
            override fun run() = parent.run()

            override fun onCompleted(taskManager: TaskManager) {
                parent.onCompleted(taskManager)
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(success.name, now)
            }

            override fun onFailed(e: Exception, taskManager: TaskManager) {
                parent.onFailed(e, taskManager)
                // Note: no attempt of retry
                val now = taskManager.dateTimeService.now()
                taskManager.schedule(failure.name, now)
            }
        }
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

    fun build(): CustomTask {
        return object : CustomTask(TaskName(name), retryPolicy) {
            override fun run() =  func()
            override fun onCompleted(taskManager: TaskManager) = onCompletedHandler(taskManager)
            override fun onFailed(e: Exception, taskManager: TaskManager) = onFailedHandler(e, taskManager)
        }
    }
}