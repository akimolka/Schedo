package org.schedo.repository.postgres

import kotlinx.serialization.json.Json
import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
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

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo?) {
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

        val infoJson = info?.let { json.encodeToString(AdditionalInfo.serializer(), it) }

        dataSource.connection.use { conn ->
            conn.prepareStatement(updateSQL).use { pstmt ->
                pstmt.setString(1, status.name)
                pstmt.setObject(2, moment)
                if (infoJson != null) pstmt.setString(3, infoJson)
                else pstmt.setNull(3, java.sql.Types.VARCHAR)
                pstmt.setString(4, instance.value)
                pstmt.executeUpdate()
            }
        }
    }

    override fun finishedTasks(): List<FinishedTask> {
        val sql = """
            WITH names AS (
              SELECT id, name
              FROM SchedoTasks
            )
            SELECT id, name, status, finishedAt, additionalInfo
            FROM SchedoStatus
              JOIN names USING (id)
            WHERE finishedAt IS NOT NULL
        """.trimIndent()

        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<FinishedTask>()
                    while (rs.next()) {
                        val instanceId = TaskInstanceID(rs.getString("id"))
                        val taskName   = TaskName(rs.getString("name"))
                        val status     = Status.valueOf(rs.getString("status"))
                        val finishedAt = rs.getObject(
                            "finishedAt", OffsetDateTime::class.java
                        )

                        val infoJson = rs.getString("additionalInfo")
                        val additionalInfo = if (infoJson.isNullOrBlank()) {
                            AdditionalInfo()
                        } else {
                            json.decodeFromString<AdditionalInfo>(infoJson)
                        }

                        results += FinishedTask(
                            instanceID     = instanceId,
                            taskName       = taskName,
                            status         = status,
                            finishedAt     = finishedAt,
                            additionalInfo = additionalInfo
                        )
                    }
                    results
                }
            }
        }
    }

    override fun taskHistory(taskName: TaskName): List<StatusEntry> {
        val sql = """
            WITH ids AS (
              SELECT id
              FROM SchedoTasks
              WHERE name = ?
            )
            SELECT *
            FROM SchedoStatus
              JOIN ids USING (id)
            ORDER BY scheduledAt DESC
        """.trimIndent()

        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, taskName.value)
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<StatusEntry>()
                    while (rs.next()) {
                        val infoJson = rs.getString("additionalInfo")
                        val additionalInfo = if (infoJson.isNullOrBlank()) {
                            AdditionalInfo()
                        } else {
                            json.decodeFromString<AdditionalInfo>(infoJson)
                        }

                        results += StatusEntry(
                            instance = TaskInstanceID(rs.getString("id")),
                            status = Status.valueOf(rs.getString("status")),
                            scheduledAt = rs.getObject("scheduledAt", OffsetDateTime::class.java),
                            enqueuedAt = rs.getObject("scheduledAt", OffsetDateTime::class.java),
                            startedAt = rs.getObject("scheduledAt", OffsetDateTime::class.java),
                            finishedAt = rs.getObject("scheduledAt", OffsetDateTime::class.java),
                            info = additionalInfo,
                        )
                    }
                    results
                }
            }
        }
    }
}