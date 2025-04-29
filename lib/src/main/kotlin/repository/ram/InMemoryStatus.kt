package repository.ram

import repository.StatusRepository
import repository.TaskResult
import task.TaskInstanceID
import java.time.OffsetDateTime

class InMemoryStatus : StatusRepository {
    override fun schedule(instance: TaskInstanceID, moment: OffsetDateTime) {}
    override fun enqueue(instance: TaskInstanceID, moment: OffsetDateTime) {}
    override fun start(instance: TaskInstanceID, moment: OffsetDateTime) {}
    override fun finish(instance: TaskInstanceID, result: TaskResult, moment: OffsetDateTime) {}
}