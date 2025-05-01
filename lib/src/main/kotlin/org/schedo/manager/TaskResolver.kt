package org.schedo.manager

import org.schedo.task.Task
import org.schedo.task.TaskName
import java.util.concurrent.ConcurrentHashMap
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class TaskResolver() {
    private val mapping = ConcurrentHashMap<TaskName, Task>()

    fun addTask(task: Task) {
        mapping[task.name] = task
    }

    fun getTask(name: TaskName): Task? {
        val task = mapping[name]
        if (task == null) {
            logger.error { "Task $name not found in TaskResolver" }
        }
        return task
    }
}