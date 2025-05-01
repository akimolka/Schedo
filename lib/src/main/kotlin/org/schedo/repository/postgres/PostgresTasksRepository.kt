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

    override fun add(instance: ScheduledTaskInstance) {
        val insertSQL = """
            INSERT INTO SchedoTasks (id, name, time)
            VALUES (?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setObject(1, instance.id.value)
                pstmt.setString(2, instance.name.value)
                pstmt.setObject(3, instance.executionTime)
                pstmt.executeUpdate()
            }
        }
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
                            TaskInstanceID(rs.getObject("id", UUID::class.java)),
                            TaskName(rs.getString("name")))
                    }
                    instances
                }
            }
        }
    }
}
