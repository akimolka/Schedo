package org.schedo.repository

import org.schedo.repository.postgres.PostgresStatusRepository
import org.schedo.repository.postgres.PostgresTasksRepository
import org.schedo.repository.ram.InMemoryStatus
import org.schedo.repository.ram.InMemoryTasks
import javax.sql.DataSource

sealed interface RepositoryType {
    data object InMemory : RepositoryType
    class Postgres(val dataSource: DataSource) : RepositoryType
}

class Repository(repositoryType: RepositoryType) {
    val tasksRepository: TasksRepository = when (repositoryType) {
        is RepositoryType.InMemory -> org.schedo.repository.ram.InMemoryTasks()
        is RepositoryType.Postgres -> PostgresTasksRepository(repositoryType.dataSource)
    }
    val statusRepository: StatusRepository = when (repositoryType) {
        is RepositoryType.InMemory -> InMemoryStatus()
        is RepositoryType.Postgres -> PostgresStatusRepository(repositoryType.dataSource)
    }
}