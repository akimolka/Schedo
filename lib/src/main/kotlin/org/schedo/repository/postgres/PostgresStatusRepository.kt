package org.schedo.repository.postgres

import org.schedo.repository.Status
import org.schedo.repository.StatusRepository
import org.schedo.repository.TaskResult
import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime
import javax.sql.DataSource

class PostgresStatusRepository (
    private val dataSource: DataSource
) : StatusRepository {

    init {
        createStatusTable(dataSource)
    }

    override fun schedule(instance: TaskInstanceID, moment: OffsetDateTime) {
        val insertSQL = """
            INSERT INTO SchedoStatus (id, status, scheduledAt)
            VALUES(?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setObject(1, instance.value)
                pstmt.setString(2, Status.SCHEDULED.name)
                pstmt.setObject(3, moment)
                pstmt.executeUpdate()
            }
        }
    }

    override fun enqueue(instance: TaskInstanceID, moment: OffsetDateTime) =
        updateStatus(instance, Status.ENQUEUED, "enqueuedAt",  moment)

    override fun start(instance: TaskInstanceID, moment: OffsetDateTime) =
        updateStatus(instance, Status.STARTED, "startedAt",  moment)

    override fun finish(instance: TaskInstanceID, result: TaskResult, moment: OffsetDateTime) {
        val status = when (result) {
            is TaskResult.Success -> Status.COMPLETED
            is TaskResult.Failed  -> Status.FAILED
        }
        updateStatus(instance, status, "finishedAt", moment)
    }

    private fun updateStatus(
        instance: TaskInstanceID,
        status: Status,
        column: String,
        moment: OffsetDateTime,
    ) {
        val updateSQL = """
            UPDATE SchedoStatus
            SET status = ?, $column = ?
            WHERE id = ?
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(updateSQL).use { pstmt ->
                pstmt.setString(1, status.name)
                pstmt.setObject(2, moment)
                pstmt.setObject(3, instance.value)
                pstmt.executeUpdate()
            }
        }
    }
}