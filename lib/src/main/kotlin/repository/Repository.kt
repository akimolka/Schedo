package repository

import repository.postgres.PostgresStatusRepository
import repository.postgres.PostgresTasksRepository
import repository.postgres.PostgresRetryRepository
import repository.ram.InMemoryStatus
import repository.ram.InMemoryTasks
import repository.ram.InMemoryRetry
import javax.sql.DataSource

sealed interface RepositoryType {
    data object InMemory : RepositoryType
    class Postgres(val dataSource: DataSource) : RepositoryType
}

class Repository(repositoryType: RepositoryType) {
    val tasksRepository: TasksRepository = when (repositoryType) {
        is RepositoryType.InMemory -> InMemoryTasks()
        is RepositoryType.Postgres -> PostgresTasksRepository(repositoryType.dataSource)
    }
    val statusRepository: StatusRepository = when (repositoryType) {
        is RepositoryType.InMemory -> InMemoryStatus()
        is RepositoryType.Postgres -> PostgresStatusRepository(repositoryType.dataSource)
    }
    val retryRepository: RetryRepository = when (repositoryType) {
        is RepositoryType.InMemory -> InMemoryRetry()
        is RepositoryType.Postgres -> PostgresRetryRepository(repositoryType.dataSource)
    }
}