package org.schedo.repository

import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime

data class ScheduledTaskInstance(val id: TaskInstanceID, val name: TaskName, val executionTime: OffsetDateTime)

interface TasksRepository {
    /**
     * Adds the given instance to the repository of scheduled tasks.
     * @return Whether the instance was added.
     */
    fun add(instance: ScheduledTaskInstance): Boolean
    fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName>
}