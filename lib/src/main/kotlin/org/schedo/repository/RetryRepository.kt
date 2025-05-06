package repository

import org.schedo.repository.Status
import java.time.OffsetDateTime
import org.schedo.task.TaskName

interface RetryRepository {
    fun getFailedCount(name: TaskName, limit: UInt): UInt {
        val statuses = getNLast(name, limit)
        val idx = statuses.indexOfFirst{ it != Status.FAILED}
        return if (idx >= 0) {
            idx.toUInt()
        } else {
            minOf(statuses.size.toUInt(), limit)
        }
    }

    fun getNLast(name: TaskName, count: UInt): List<Status>
}