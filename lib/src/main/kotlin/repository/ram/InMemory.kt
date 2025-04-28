package repository.ram

import task.TaskName
import java.time.OffsetDateTime
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryRepository: Repository {
    private val tasks = CopyOnWriteArrayList<TaskEntity>()

    override fun add(task: TaskEntity) {
        tasks.add(task)
    }

    override fun pickTaskNamesDue(timePoint: OffsetDateTime): List<TaskName> {
        val picked = tasks.filter { it.executionTime.isBefore(timePoint) }
        tasks.removeAll(picked.toSet())
        return picked.map { it.name }
    }
}