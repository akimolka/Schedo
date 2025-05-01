package org.schedo.repository

import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime

enum class Status {
    SCHEDULED, ENQUEUED, STARTED, COMPLETED, FAILED
}

interface StatusRepository {
    fun schedule(instance: TaskInstanceID, moment: OffsetDateTime)
    fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime)
}