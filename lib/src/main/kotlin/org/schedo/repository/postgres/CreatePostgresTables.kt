package org.schedo.repository.postgres

import javax.sql.DataSource

private fun createTasksTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                id VARCHAR(255) PRIMARY KEY,
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

private fun createStatusTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoStatus (
                id VARCHAR(255) PRIMARY KEY,
                status VARCHAR(20),
                scheduledAt TIMESTAMP WITH TIME ZONE NOT NULL,
                enqueuedAt TIMESTAMP WITH TIME ZONE NULL,
                startedAt TIMESTAMP WITH TIME ZONE NULL,
                finishedAt TIMESTAMP WITH TIME ZONE NULL,
                FOREIGN KEY(id) REFERENCES SchedoTasks(id)
            )
        """.trimIndent()

    dataSource.connection.use { connection ->
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
    }
}

fun createPostgresTables(dataSource: DataSource) {
    createTasksTable(dataSource)
    createStatusTable(dataSource)
}