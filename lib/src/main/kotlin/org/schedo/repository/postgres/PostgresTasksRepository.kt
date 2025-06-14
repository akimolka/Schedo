package org.schedo.repository.postgres

import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.TasksRepository
import org.schedo.task.TaskInstanceName
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime


class PostgresTasksRepository(
    private val transactionManager: DataSourceTransaction
) : TasksRepository {

    override fun add(instance: ScheduledTaskInstance): Boolean {
        val insertSQL = """
            INSERT INTO SchedoTasks (id, name, time)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """.trimIndent()

        val connection = transactionManager.getConnection()
        val rowsInserted = connection.prepareStatement(insertSQL).use { pstmt ->
            pstmt.setString(1, instance.id.value)
            pstmt.setString(2, instance.name.value)
            pstmt.setObject(3, instance.executionTime)
            pstmt.executeUpdate()
        }

        return rowsInserted > 0
    }

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceName> {
        val sql = """
            WITH due AS (
              SELECT id
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

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { pstmt ->
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

    override fun listTaskInstancesDue(timePoint: OffsetDateTime): List<ScheduledTaskInstance> {
        val sql = """
            SELECT id, name, time
              FROM SchedoTasks
              WHERE time <= ? 
                AND picked = FALSE
        """.trimIndent()

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setObject(1, timePoint)
            pstmt.executeQuery().use { rs ->
                val instances = mutableListOf<ScheduledTaskInstance>()
                while (rs.next()) {
                    instances += ScheduledTaskInstance(
                        TaskInstanceID(rs.getString("id")),
                        TaskName(rs.getString("name")),
                        rs.getObject("time", OffsetDateTime::class.java),
                    )
                }
                instances
            }
        }
    }

    override fun countTaskInstancesDue(timePoint: OffsetDateTime): Int {
        // TODO can remove override and use
        val countSql = """
          SELECT COUNT(id)
          FROM SchedoTasks
          WHERE time <= ? 
            AND picked = FALSE
        """.trimIndent()

        val connection = transactionManager.getConnection()
        return connection.prepareStatement(countSql).use { pstmt ->
            pstmt.setObject(1, timePoint)
            pstmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}
