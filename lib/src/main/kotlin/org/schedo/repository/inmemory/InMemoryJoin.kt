package org.schedo.repository.inmemory

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.util.concurrent.ConcurrentHashMap

class InMemoryJoin {
    private val taskToInstances = ConcurrentHashMap<TaskName, MutableList<TaskInstanceID>>()
    private val instanceToTasks = ConcurrentHashMap<TaskInstanceID, TaskName>()

    fun add(instanceID: TaskInstanceID, taskName: TaskName) {
        taskToInstances.computeIfAbsent(taskName) { mutableListOf() }
            .add(instanceID)
        instanceToTasks[instanceID] = taskName
    }

    fun taskInstances(taskName: TaskName): List<TaskInstanceID> =
        taskToInstances[taskName]?.toList().orEmpty()

    fun taskName(instanceID: TaskInstanceID): TaskName? =
        instanceToTasks[instanceID]
}