package repository

import task.TaskInstanceFullName
import task.TaskInstanceID
import task.TaskName
import java.time.OffsetDateTime

data class ScheduledTaskInstance(val id: TaskInstanceID, val name: TaskName, val executionTime: OffsetDateTime)

interface TasksRepository {
    fun add(instance: ScheduledTaskInstance)
    fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceFullName>
}