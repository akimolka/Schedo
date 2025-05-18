package org.schedo.repository.postgres

import kotlinx.serialization.json.Json
import org.schedo.repository.AdditionalInfo
import org.schedo.repository.Status
import org.schedo.repository.StatusRepository
import org.schedo.task.TaskInstanceID
import java.time.OffsetDateTime
import javax.sql.DataSource

class PostgresStatusRepository (
    private val dataSource: DataSource
) : StatusRepository {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    override fun insert(instance: TaskInstanceID, moment: OffsetDateTime) {
        val insertSQL = """
            INSERT INTO SchedoStatus (id, status, scheduledAt)
            VALUES(?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setString(1, instance.value)
                pstmt.setString(2, Status.SCHEDULED.name)
                pstmt.setObject(3, moment)
                pstmt.executeUpdate()
            }
        }
    }

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo) {
        val column = when (status) {
            Status.SCHEDULED -> "scheduledAt"
            Status.ENQUEUED -> "enqueuedAt"
            Status.STARTED -> "startedAt"
            Status.COMPLETED -> "finishedAt"
            Status.FAILED -> "finishedAt"
        }

        val updateSQL = """
            UPDATE SchedoStatus
            SET status = ?,
                $column = ?,
                additionalInfo = ?
            WHERE id = ?
        """.trimIndent()

        val infoJson: String = json.encodeToString(AdditionalInfo.serializer(), info)

        dataSource.connection.use { conn ->
            conn.prepareStatement(updateSQL).use { pstmt ->
                pstmt.setString(1, status.name)
                pstmt.setObject(2, moment)
                pstmt.setString(3, infoJson)
                pstmt.setString(4, instance.value)
                pstmt.executeUpdate()
            }
        }
    }
}