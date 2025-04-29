package repository.ram

import repository.ScheduledTaskInstance
import repository.TasksRepository
import task.TaskInstanceFullName
import task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryTasks: TasksRepository {
    private val tasks = CopyOnWriteArrayList<ScheduledTaskInstance>()

    override fun add(instance: ScheduledTaskInstance) {
        tasks.add(instance)
    }

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceFullName> {
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked.toSet())
        return picked.map { TaskInstanceFullName(it.id, it.name) }
    }
}