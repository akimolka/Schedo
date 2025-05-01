package org.schedo.repository.inmemory

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TasksRepository
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryTasks: TasksRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTaskInstance>()

    override fun add(instance: ScheduledTaskInstance) {
        tasks.add(instance)
    }

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName> {
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked.toSet())
        return picked.map { TaskInstanceName(it.id, it.name) }
    }
}