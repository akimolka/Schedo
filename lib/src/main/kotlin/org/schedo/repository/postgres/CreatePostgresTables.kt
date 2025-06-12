package org.schedo.repository.postgres

import javax.sql.DataSource

private fun createTasksTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoTasks (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
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
                status VARCHAR(20) NOT NULL,
                scheduledFor TIMESTAMP WITH TIME ZONE NOT NULL,
                createdAt TIMESTAMP WITH TIME ZONE NOT NULL,
                enqueuedAt TIMESTAMP WITH TIME ZONE NULL,
                startedAt TIMESTAMP WITH TIME ZONE NULL,
                finishedAt TIMESTAMP WITH TIME ZONE NULL,
                additionalInfo TEXT NULL,
                FOREIGN KEY(id) REFERENCES SchedoTasks(id)
            )
        """.trimIndent()

    dataSource.connection.use { connection ->
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(createTableSQL)
        }
    }
}


// TODO name references what?
private fun createExecutionTable(dataSource: DataSource) {
    val createTableSQL = """
            CREATE TABLE IF NOT EXISTS SchedoExecutions (
                name VARCHAR(255) PRIMARY KEY,
                currentInstance VARCHAR(255) NOT NULL,
                status VARCHAR(20) NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0,
                cancelled BOOLEAN NOT NULL DEFAULT FALSE,
                cancelledAt TIMESTAMP WITH TIME ZONE NULL,
                FOREIGN KEY(currentInstance) REFERENCES SchedoTasks(id),
                CONSTRAINT retryCount check (retryCount >= 0)
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
    createExecutionTable(dataSource)
}