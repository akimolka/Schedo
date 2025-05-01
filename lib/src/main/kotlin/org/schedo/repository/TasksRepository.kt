package org.schedo.repository

import org.schedo.task.TaskInstanceFullName
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime

data class ScheduledTaskInstance(val id: TaskInstanceID, val name: TaskName, val executionTime: OffsetDateTime)

interface TasksRepository {
    fun add(instance: ScheduledTaskInstance)
    fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceFullName>
}