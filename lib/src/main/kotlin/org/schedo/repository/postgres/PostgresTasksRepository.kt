package org.schedo.repository.postgres

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TasksRepository
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import java.util.*
import javax.sql.DataSource


class PostgresTasksRepository(
    private val dataSource: DataSource
) : TasksRepository {

    override fun add(instance: ScheduledTaskInstance): Boolean {
        val insertSQL = """
            INSERT INTO SchedoTasks (id, name, time)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """.trimIndent()

        val rowsInserted = dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setString(1, instance.id.value)
                pstmt.setString(2, instance.name.value)
                pstmt.setObject(3, instance.executionTime)
                pstmt.executeUpdate()
            }
        }

        return rowsInserted > 0
    }

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName> {
        val sql = """
            WITH due AS (
              SELECT id, name
              FROM SchedoTasks
              WHERE time <= ? 
                AND picked = FALSE
              FOR UPDATE SKIP LOCKED
            )
            UPDATE SchedoTasks
            SET picked = TRUE
            WHERE id IN (SELECT id FROM due)
            RETURNING id, name
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setObject(1, timePoint)
                pstmt.executeQuery().use { rs ->
                    val instances = mutableListOf<TaskInstanceName>()
                    while (rs.next()) {
                        instances += TaskInstanceName(
                            TaskInstanceID(rs.getString("id")),
                            TaskName(rs.getString("name")))
                    }
                    instances
                }
            }
        }
    }

    override fun countTaskInstancesDue(timePoint: OffsetDateTime): Int {
        val countSql = """
            WITH due AS (
              SELECT COUNT(id)
              FROM SchedoTasks
              WHERE time <= ? 
                AND picked = FALSE
            )
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(countSql).use { pstmt ->
                pstmt.setObject(1, timePoint)
                pstmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }
}
