package org.schedo.repository.inmemory

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TasksRepository
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryTasks(
    val inMemoryJoin: InMemoryJoin
): TasksRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTaskInstance>()

    override fun add(instance: ScheduledTaskInstance): Boolean {
        inMemoryJoin.add(instance.id, instance.name)
        tasks.add(instance)
        return true
    }

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName> {
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked.toSet())
        return picked.map { TaskInstanceName(it.id, it.name) }
    }

    override fun listTaskInstancesDue(timePoint: OffsetDateTime): List<ScheduledTaskInstance> {
        return tasks.filter { it.executionTime.isBefore(timePoint) }
    }
}