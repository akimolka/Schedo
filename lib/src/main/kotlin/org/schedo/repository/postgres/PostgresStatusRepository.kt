package org.schedo.repository.postgres

import kotlinx.serialization.json.Json
import org.schedo.repository.*
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime

class PostgresStatusRepository (
    private val transactionManager: DataSourceTransaction
) : StatusRepository {
    private val json = Json { prettyPrint = false; encodeDefaults = true }

    override fun insert(instance: TaskInstanceID, moment: OffsetDateTime) {
        val insertSQL = """
            INSERT INTO SchedoStatus (id, status, scheduledAt)
            VALUES(?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """.trimIndent()

        val connection = transactionManager.getConnection()
        connection.prepareStatement(insertSQL).use { pstmt ->
            pstmt.setString(1, instance.value)
            pstmt.setString(2, Status.SCHEDULED.name)
            pstmt.setObject(3, moment)
            pstmt.executeUpdate()
        }
    }

    override fun updateStatus(status: Status, instance: TaskInstanceID, moment: OffsetDateTime, info: AdditionalInfo?) {
        val timeColumn = when (status) {
            Status.SCHEDULED -> "scheduledAt"
            Status.ENQUEUED  -> "enqueuedAt"
            Status.STARTED   -> "startedAt"
            Status.COMPLETED, Status.FAILED, Status.CANCELLED -> "finishedAt"
        }

        // Entry associated with this instance won't be changed by other servers
        // So SELECT ... FOR UPDATE is unnecessary
        val readInfoSql = """
        SELECT additionalInfo
            FROM SchedoStatus
            WHERE id = ?
        """.trimIndent()

        val updateBase = """
        UPDATE SchedoStatus
           SET status = ?,
               $timeColumn = ?
        """.trimIndent()

        // If info is null, additionalInfo in SchedoStatus in not updated
        // Otherwise, previous additionalInfo is read, values are merged and new value is written
        val updateSqlWithInfo = """
            $updateBase,
                additionalInfo = ?
            WHERE id = ?
        """.trimIndent()

        val updateSqlWithoutInfo = """
            $updateBase
            WHERE id = ?
        """.trimIndent()

        val connection = transactionManager.getConnection()
        val mergedInfoJson: String? = if (info != null) {
            var prevInfo: AdditionalInfo? = null
            connection.prepareStatement(readInfoSql).use { ps ->
                ps.setString(1, instance.value)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        prevInfo = loadAdditionalInfo(rs)
                    }
                }
            }
            mergeInfo(prevInfo, info)
                ?.let { json.encodeToString(AdditionalInfo.serializer(), it) }
        } else {
            null
        }

        val sql = if (info != null) updateSqlWithInfo else updateSqlWithoutInfo
        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, status.name)         // status
            ps.setObject(2, moment)              // timestamp column

            if (info != null) {
                // bind merged additionalInfo JSON
                ps.setString(3, mergedInfoJson)
                ps.setString(4, instance.value)  // WHERE id=?
            } else {
                // no additionalInfo param, so idx=3 is id
                ps.setString(3, instance.value)
            }
            ps.executeUpdate()
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

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { ps ->
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

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { ps ->
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

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { ps ->
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