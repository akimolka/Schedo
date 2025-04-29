package repository

import task.TaskInstanceID
import java.time.Duration
import java.time.OffsetDateTime

sealed interface TaskResult {
    class Success(val spendingTime: Duration) : TaskResult
    class Failed(val e: Exception) : TaskResult
}

enum class Status {
    SCHEDULED, ENQUEUED, STARTED, COMPLETED, FAILED
}

interface StatusRepository {
    fun schedule(instance: TaskInstanceID, moment: OffsetDateTime)
    fun enqueue(instance: TaskInstanceID, moment: OffsetDateTime)
    fun start(instance: TaskInstanceID, moment: OffsetDateTime)
    fun finish(instance: TaskInstanceID, result: TaskResult, moment: OffsetDateTime)
}