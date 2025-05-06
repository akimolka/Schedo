package repository.ram

import repository.RetryRepository
import org.schedo.repository.Status
import org.schedo.task.TaskName
import java.time.OffsetDateTime

class InMemoryRetry: RetryRepository {
    // TODO

    override fun getNLast(name: TaskName, count: Int): List<Status> {
       return emptyList()
    }
}