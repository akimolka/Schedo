package repository.postgres

import repository.RetryRepository
import org.schedo.repository.Status
import org.schedo.task.TaskName
import java.time.OffsetDateTime
import javax.sql.DataSource

class PostgresRetryRepository (
    private val dataSource: DataSource
) : RetryRepository {
    // TODO хранить количество падений
    private val lastNFinishedQuery = """
            WITH ids AS (
              SELECT id
              FROM SchedoTasks
              WHERE name = ?
            )
            SELECT status, finishedAt
            FROM SchedoStatus
              JOIN ids USING (id)
            WHERE finishedAt IS NOT NULL
            ORDER BY finishedAt DESC
            LIMIT ?
        """.trimIndent()

    override fun getLastFail(name: TaskName): OffsetDateTime? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(lastNFinishedQuery).use { pstmt ->
                pstmt.setString(1, name.value)
                pstmt.setInt(2, 1)
                pstmt.executeQuery().use { rs ->
                    if (!rs.next()) return null

                    val status = rs.getString("status")
                    if (status == Status.FAILED.name) {
                        rs.getObject("finishedAt", OffsetDateTime::class.java)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override fun getNLast(name: TaskName, count: Int): List<Status> {
        return dataSource.connection.use { conn ->
            conn.prepareStatement(lastNFinishedQuery).use { ps ->
                ps.setString(1, name.value)
                ps.setInt(2, count)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<Status>()
                    while (rs.next()) {
                        result += Status.valueOf(rs.getString("status"))
                    }
                    result
                }
            }
        }
    }
}