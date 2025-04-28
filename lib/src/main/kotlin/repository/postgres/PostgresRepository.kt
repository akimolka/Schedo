package repository.postgres

import repository.TasksRepository
import repository.TaskEntity
import task.TaskName
import java.time.OffsetDateTime
import javax.sql.DataSource


class PostgresTasksRepository(
    private val dataSource: DataSource
) : TasksRepository {

    init {
        createTasksTable(dataSource)
    }

    override fun add(task: TaskEntity) {
        val insertSQL = """
            INSERT INTO SchedoTasks (name, time)
            VALUES (?, ?)
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setString(1, task.name.value)
                pstmt.setObject(2, task.executionTime)
                pstmt.executeUpdate()
            }
        }
    }

    override fun pickTaskNamesDue(timePoint: OffsetDateTime): List<TaskName> {
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
            RETURNING name
        """.trimIndent()

        return dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setObject(1, timePoint)
                pstmt.executeQuery().use { rs ->
                    val names = mutableListOf<TaskName>()
                    while (rs.next()) {
                        names += TaskName(rs.getString("name"))
                    }
                    names
                }
            }
        }
    }
}
