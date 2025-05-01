package org.schedo.scheduler

import org.schedo.manager.TaskManager
import org.schedo.repository.*
import org.schedo.repository.inmemory.InMemoryStatus
import org.schedo.repository.inmemory.InMemoryTasks
import org.schedo.repository.postgres.PostgresStatusRepository
import org.schedo.repository.postgres.PostgresTasksRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sql.DataSource

class SchedulerBuilder {
    private var executor = Executors.newCachedThreadPool()
    private var dataSource: DataSource? = null  // null stands for InMemory
    private var dataSourceType: DataSourceType? = null  // null stands for InMemory

    fun dataSource(dataSource: DataSource): SchedulerBuilder {
        this.dataSource = dataSource
        this.dataSourceType = DataSourceType.autodetect(dataSource)
        return this
    }

    fun dataSource(dataSource: DataSource, dataSourceType: DataSourceType): SchedulerBuilder {
        this.dataSource = dataSource
        this.dataSourceType = dataSourceType
        return this
    }

    fun executor(executor: ExecutorService): SchedulerBuilder {
        this.executor = executor
        return this
    }

    fun build(): Scheduler {
        val dsType = dataSourceType  // snapshot it
        val tasksRepository: TasksRepository = when (dsType) {
            null -> InMemoryTasks()
            is DataSourceType.Postgres -> PostgresTasksRepository(dataSource!!)
            is DataSourceType.Other -> TODO("${dsType.name} is not supported")
        }
        val statusRepository: StatusRepository = when (dsType) {
            null -> InMemoryStatus()
            is DataSourceType.Postgres -> PostgresStatusRepository(dataSource!!)
            is DataSourceType.Other -> TODO("${dsType.name} is not supported")
        }
        val taskManager = TaskManager(tasksRepository, statusRepository)
        return Scheduler(taskManager, executor)
    }
}