package org.schedo.repository.postgres

import kotlinx.serialization.json.Json
import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.sql.ResultSet
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

                        results += FinishedTask(
                            instanceID     = instanceId,
                            taskName       = taskName,
                            status         = status,
                            finishedAt     = finishedAt,
                            additionalInfo = loadAdditionalInfo(rs)
                        )
                    }
                    results
                }
            }
        }
    }

    override fun taskHistory(taskName: TaskName, from: OffsetDateTime, to: OffsetDateTime): List<StatusEntry> {
        val sql = """
            WITH ids AS (
              SELECT id
              FROM SchedoTasks
              WHERE name = ?
            )
            SELECT *
            FROM SchedoStatus
              JOIN ids USING (id)
            WHERE ? <= scheduledAt AND scheduledAt <= ?
            ORDER BY scheduledAt DESC
        """.trimIndent()

        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, taskName.value)
                ps.setObject(2, from)
                ps.setObject(3, to)
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<StatusEntry>()
                    while (rs.next()) {
                        results += loadRow(rs)
                    }
                    results
                }
            }
        }
    }

    override fun history(from: OffsetDateTime, to: OffsetDateTime): List<Pair<TaskName, StatusEntry>> {
        val sql = """
            WITH names AS (
              SELECT id, name
              FROM SchedoTasks
            )
            SELECT *
            FROM SchedoStatus
              JOIN names USING (id)
            WHERE ? <= scheduledAt AND scheduledAt <= ?
            ORDER BY scheduledAt DESC
        """.trimIndent()

        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setObject(1, from)
                ps.setObject(2, to)
                ps.executeQuery().use { rs ->
                    val results = mutableListOf<Pair<TaskName, StatusEntry>>()
                    while (rs.next()) {
                        results += Pair(TaskName(rs.getString("name")), loadRow(rs))
                    }
                    results
                }
            }
        }
    }

    private fun loadAdditionalInfo(rs: ResultSet): AdditionalInfo? {
        val infoJson = rs.getString("additionalInfo")
        return if (infoJson.isNullOrBlank()) {
            null
        } else {
            json.decodeFromString<AdditionalInfo>(infoJson)
        }
    }

    private fun loadRow(rs: ResultSet): StatusEntry {
        return StatusEntry(
            instance = TaskInstanceID(rs.getString("id")),
            status = Status.valueOf(rs.getString("status")),
            scheduledAt = rs.getObject("scheduledAt", OffsetDateTime::class.java),
            enqueuedAt = rs.getObject("enqueuedAt", OffsetDateTime::class.java),
            startedAt = rs.getObject("startedAt", OffsetDateTime::class.java),
            finishedAt = rs.getObject("finishedAt", OffsetDateTime::class.java),
            info = loadAdditionalInfo(rs),
        )
    }
}