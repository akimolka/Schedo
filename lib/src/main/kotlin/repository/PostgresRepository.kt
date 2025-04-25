package repository

import task.TaskName
import java.time.OffsetDateTime
import javax.sql.DataSource


class PostgresRepository(
    private val dataSource: DataSource
) : Repository {

    init {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                name VARCHAR(255) PRIMARY KEY,
                time TIME NOT NULL,
                picked BOOLEAN NOT NULL DEFAULT FALSE
            )
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.executeUpdate(createTableSQL)
            }
        }
    }

    override fun add(task: TaskEntity) {
        val insertSQL = """
            INSERT INTO SchedoTasks (name, time)
            VALUES (?, ?)
            ON CONFLICT (name) DO NOTHING
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

        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { pstmt ->
                    pstmt.setObject(1, timePoint)
                    pstmt.executeQuery().use { rs ->
                        val names = mutableListOf<TaskName>()
                        while (rs.next()) {
                            names += TaskName(rs.getString("name"))
                        }
                        connection.commit()
                        names
                    }
                }
            } catch (ex: java.sql.SQLException) {
                connection.rollback()
                throw ex
            }
        }
    }
}
