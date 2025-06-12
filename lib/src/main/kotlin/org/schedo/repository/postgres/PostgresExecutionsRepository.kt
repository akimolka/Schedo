package org.schedo.repository.postgres

import org.schedo.repository.ExecutionsRepository
import org.schedo.repository.TaskStatus
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName

class PostgresExecutionsRepository (
    private val transactionManager: DataSourceTransaction
) : ExecutionsRepository {
    override fun setRetryCount(task: TaskName, value: Int) {
        val insertSQL = """
            UPDATE SchedoExecutions
            SET retryCount = ?
            WHERE name = ?
        """.trimIndent()

        val connection = transactionManager.getConnection()
        connection.prepareStatement(insertSQL).use { pstmt ->
            pstmt.setInt(1, value)
            pstmt.setString(2, task.value)
            pstmt.executeUpdate()
        }
    }

    override fun addRetryCount(task: TaskName, delta: Int) {
        val sql = """
            UPDATE SchedoExecutions
            SET retryCount = retryCount + ?
            WHERE name = ?
        """.trimIndent()

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, delta)
            pstmt.setString(2, task.value)
            pstmt.executeUpdate()
        }
    }

    override fun getRetryCount(task: TaskName): UInt {
        val sql = """
            SELECT retryCount
            FROM SchedoExecutions
            WHERE name = ?
        """.trimIndent()

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.executeQuery().use { rs ->
                if (rs.next()) {
                    rs.getInt("retryCount").toUInt()
                } else {
                    0U
                }
            }
        }
    }

    override fun updateStatus(task: TaskName, instanceID: TaskInstanceID, status: TaskStatus) {
        if (status != TaskStatus.RUNNING && status != TaskStatus.FINISHED) {
            return
        }

        val conn = transactionManager.getConnection()

        // Check current instance before updating for TaskStatus.FINISHED
        if (status == TaskStatus.FINISHED) {
            val currentSQL = "SELECT currentInstance FROM SchedoExecutions WHERE name = ?"
            val currentInstance = conn.prepareStatement(currentSQL).use { pstmt ->
                pstmt.setString(1, task.value)
                pstmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("currentInstance") else null
                }
            }

            if (currentInstance != null && currentInstance != instanceID.value) {
                // outdated
                return
            }
        }

        // Update current instance (actual update only if RUNNING) and status
        val sql = """
            INSERT INTO SchedoExecutions (name, currentInstance, status)
            VALUES (?, ?, ?)
            ON CONFLICT (name) DO UPDATE
            SET currentInstance = EXCLUDED.currentInstance,
                status = EXCLUDED.status
        """.trimIndent()

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.setString(2, instanceID.value)
            pstmt.setString(3, status.name)
            pstmt.executeUpdate()
        }
    }

    override fun cancel(task: TaskName): Boolean {
        val sql = """
            UPDATE SchedoExecutions
            SET cancelled = TRUE
            WHERE name = ? AND cancelled <> TRUE
        """.trimIndent()

        val conn = transactionManager.getConnection()
        return conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.executeUpdate() > 0
        }
    }

    override fun clearCancelled(task: TaskName): Boolean {
        val sql = """
            UPDATE SchedoExecutions
            SET cancelled = FALSE
            WHERE name = ? AND cancelled <> FALSE
        """.trimIndent()

        val conn = transactionManager.getConnection()
        return conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.executeUpdate() > 0
        }
    }

    override fun isCancelled(task: TaskName): Boolean {
        val sql = """
            UPDATE SchedoExecutions
            SET status = 'CANCELLED'
            WHERE name = ? AND cancelled = true
            RETURNING cancelled
        """.trimIndent()

        val conn = transactionManager.getConnection()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.executeQuery().use { rs ->
                return rs.next() && rs.getBoolean("cancelled")
            }
        }
    }

    /** Resume is possible only if task is stopped completely and won't reschedule, i.e.
     * if status is FINISHED or CANCELLED. After resume one should atomically switch
     * the status to RESUMED so that two competing tryResume queries resume task only once.
     */
    override fun tryResume(task: TaskName): Boolean {
        val sql = """
            UPDATE SchedoExecutions
            SET status = 'RESUMED', cancelled = false
            WHERE name = ? AND status IN ('FINISHED', 'CANCELLED')
        """.trimIndent()

        val conn = transactionManager.getConnection()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            val updatedRows = pstmt.executeUpdate()
            return updatedRows > 0
        }
    }

    override fun getStatusAndCancelled(task: TaskName): Pair<TaskStatus, Boolean>? {
        val sql = "SELECT status, cancelled FROM SchedoExecutions WHERE name = ?"
        val conn = transactionManager.getConnection()

        return conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, task.value)
            pstmt.executeQuery().use { rs ->
                if (rs.next()){
                    Pair(
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getBoolean("cancelled")
                    )
                } else {
                    null
                }
            }
        }
    }
}