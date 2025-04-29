package repository.postgres

import repository.ScheduledTaskInstance
import repository.TasksRepository
import task.TaskInstanceFullName
import task.TaskInstanceID
import task.TaskName
import java.time.OffsetDateTime
import java.util.*
import javax.sql.DataSource


class PostgresTasksRepository(
    private val dataSource: DataSource
) : TasksRepository {

    init {
        createTasksTable(dataSource)
    }

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

    override fun pickTaskInstancesDue(timePoint: OffsetDateTime): List<TaskInstanceFullName> {
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
                    val instances = mutableListOf<TaskInstanceFullName>()
                    while (rs.next()) {
                        instances += TaskInstanceFullName(
                            TaskInstanceID(rs.getObject("id", UUID::class.java)),
                            TaskName(rs.getString("name")))
                    }
                    instances
                }
            }
        }
    }
}
