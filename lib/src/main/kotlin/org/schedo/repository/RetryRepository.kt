package repository

import org.schedo.repository.Status
import java.time.OffsetDateTime
import org.schedo.task.TaskName

interface RetryRepository {
    fun getLastFail(name: TaskName): OffsetDateTime?

    fun getFailedCount(name: TaskName, limit: Int): Int {
        val statuses = getNLast(name, limit)
        val indx = statuses.indexOfFirst{ it != Status.FAILED}
        return if (indx != -1) indx else statuses.size
    }

    fun getNLast(name: TaskName, count: Int): List<Status>
}