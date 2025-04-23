package repository

import components.TaskResolver
import task.ScheduledTask
import java.sql.Connection
import java.time.OffsetTime

// TODO Where to log to?
class JDBCRepository(val connection: Connection) : Repository {
    val taskResolver = TaskResolver()

    init{
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                name VARCHAR(255) PRIMARY KEY,
                time TIME NOT NULL,
                picked BOOLEAN NOT NULL DEFAULT FALSE
            )
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
//        TODO Written by chatGPT. Why better than
//        val stmt = connection.createStatement()
//        stmt.executeUpdate(createTableSQL)
    }
    override fun add(task: ScheduledTask) {
        taskResolver.addTask(task)

        val insertSQL = """
            INSERT INTO SchedoTasks (name, time)
            VALUES (?, ?)
            ON CONFLICT (name) DO NOTHING
        """.trimIndent()

        connection.prepareStatement(insertSQL).use { pstmt ->
            pstmt.setString(1, task.name)
            pstmt.setObject(2, task.executionTime)
            pstmt.executeUpdate()
        }
    }

    private fun pickDueNames(timePoint: OffsetTime): List<String> {
        val sql = """
            WITH due AS (
              SELECT name
              FROM SchedoTasks
              WHERE time <= ? 
                AND picked = FALSE
              FOR UPDATE SKIP LOCKED
            )
            UPDATE SchedoTasks
            SET picked = TRUE
            WHERE name IN (SELECT name FROM due)
            RETURNING name
        """.trimIndent()

        val initialAutoCommit = connection.autoCommit // TODO it it true?
        connection.autoCommit = false
        try {
            connection.prepareStatement(sql).use { pstmt ->
                pstmt.setObject(1, timePoint)
                pstmt.executeQuery().use { rs ->
                    val names = mutableListOf<String>()
                    while (rs.next()) {
                        names += rs.getString("name")
                    }
                    connection.commit()
                    return names
                }
            }
        } catch (ex: java.sql.SQLException) {
            connection.rollback()
            throw ex
        } finally {
            connection.autoCommit = initialAutoCommit
        }
    }

    override fun pickDue(timePoint: OffsetTime): List<ScheduledTask> {
        return pickDueNames(timePoint).map { taskResolver.getTask(it) }.filterNotNull()
    }
}
