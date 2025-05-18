package repository.ram

import repository.RetryRepository
import org.schedo.repository.Status
import org.schedo.repository.inmemory.InMemoryStatus
import org.schedo.repository.inmemory.InMemoryTasks
import org.schedo.task.TaskName

class InMemoryRetry(
    private val tasksRepository: InMemoryTasks,
    private val statusRepository: InMemoryStatus,
): RetryRepository {

    override fun getNLast(name: TaskName, count: UInt): List<Status> {
        val instances = tasksRepository.getTaskInstances(name)
        return instances.mapNotNull { statusRepository.getStatus(it) }
            .filter { it.finishedAt != null }
            .sortedByDescending { it.finishedAt }
            .take(count.toInt())
            .map { it.status }
    }
}