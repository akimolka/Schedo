package repository.postgres

import javax.sql.DataSource

fun createTasksTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                id UUID PRIMARY KEY,
                name VARCHAR(255),
                time TIMESTAMP WITH TIME ZONE NOT NULL,
                picked BOOLEAN NOT NULL DEFAULT FALSE
            )
        """.trimIndent()

    dataSource.connection.use { connection ->
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
    }
}

fun createStatusTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoStatus (
                id UUID references SchedoTasks(id),
                status VARCHAR(20),
                scheduledAt TIMESTAMP WITH TIME ZONE NOT NULL,
                enqueuedAt TIMESTAMP WITH TIME ZONE NULL,
                startedAt TIMESTAMP WITH TIME ZONE NULL,
                finishedAt TIMESTAMP WITH TIME ZONE NULL
            )
        """.trimIndent()

    dataSource.connection.use { connection ->
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
    }
}