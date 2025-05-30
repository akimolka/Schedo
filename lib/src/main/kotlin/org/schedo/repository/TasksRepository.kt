package org.schedo.repository

import kotlinx.serialization.Serializable
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import org.schedo.util.KOffsetDateTimeSerializer


@Serializable
data class ScheduledTaskInstance(
    val id: TaskInstanceID,
    val name: TaskName,
    @Serializable(KOffsetDateTimeSerializer::class) val executionTime: OffsetDateTime
)

interface TasksRepository {
    /**
     * Adds the given instance to the repository of scheduled tasks.
     * @return Whether the instance was added.
     */
    fun add(instance: ScheduledTaskInstance): Boolean
    fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName>
    fun listTaskInstancesDue(timePoint: OffsetDateTime): List<ScheduledTaskInstance>
    fun countTaskInstancesDue(timePoint: OffsetDateTime): Int =
        listTaskInstancesDue(timePoint).size
}