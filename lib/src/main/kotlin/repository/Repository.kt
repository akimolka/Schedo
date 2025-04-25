package repository

import task.TaskName
import java.time.OffsetDateTime

data class TaskEntity(val name: TaskName, val executionTime: OffsetDateTime)

interface Repository {
    fun add(task: TaskEntity)
    fun pickTaskNamesDue(timePoint: OffsetDateTime): List<TaskName>
}