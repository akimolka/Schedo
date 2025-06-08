package org.schedo.repository.postgres

import org.schedo.repository.ExecutionsRepository
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskName
import javax.sql.DataSource

class PostgresExecutionsRepository (
    private val dataSource: DataSource
) : ExecutionsRepository {
    override fun setRetryCount(task: TaskName, value: Int) {
        val insertSQL = """
            INSERT INTO SchedoExecutions (name, retryCount)
            VALUES (?, ?)
            ON CONFLICT (name) DO UPDATE
            SET retryCount = ?
        """.trimIndent()

        val rowsInserted = dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setString(1, task.value)
                pstmt.setInt(2, value)
                pstmt.setInt(3, value)
                pstmt.executeUpdate()
            }
        }
    }

    override fun addRetryCount(task: TaskName, delta: Int) {
        val sql = """
            UPDATE SchedoExecutions
            SET retryCount = retryCount + ?
            WHERE name = ?
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setInt(1, delta)
                pstmt.setString(2, task.value)
                pstmt.executeUpdate()
            }
        }
    }

    override fun getRetryCount(task: TaskName): UInt {
        val sql = """
            SELECT retryCount
            FROM SchedoExecutions
            WHERE name = ?
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { pstmt ->
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
    }

}