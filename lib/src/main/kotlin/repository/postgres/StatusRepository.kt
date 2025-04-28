package repository.postgres

import repository.StatusRepository
import repository.TaskResult
import task.TaskName
import javax.sql.DataSource

class PostgresStatusRepository (
    private val dataSource: DataSource
) : StatusRepository {

    init {
        createStatusTable(dataSource)
    }

    override fun schedule(name: TaskName) {
        val updateSQL = """
            INSERT INTO table (name, status, scheduledAt)
            VALUES(1, "A", 19) ON DUPLICATE KEY UPDATE
            name="A", age=19
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(insertSQL).use { pstmt ->
                pstmt.setString(1, task.name.value)
                pstmt.setObject(2, task.executionTime)
                pstmt.executeUpdate()
            }
        }
    }

    override fun start(name: TaskName) {

    }

    override fun finish(name: TaskName, result: TaskResult) {

    }
}