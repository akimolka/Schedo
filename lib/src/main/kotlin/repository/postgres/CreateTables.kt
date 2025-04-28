package repository.postgres

import javax.sql.DataSource

fun createTasksTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                id SERIAL PRIMARY KEY,
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
                name VARCHAR(255) PRIMARY KEY,
                status VARCHAR(255),
                scheduledAt TIMESTAMP WITH TIME ZONE NOT NULL,
                startedAt TIMESTAMP WITH TIME ZONE NULL,
                finishedAt TIMESTAMP WITH TIME ZONE NULL,
            )
        """.trimIndent()

    dataSource.connection.use { connection ->
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
    }
}