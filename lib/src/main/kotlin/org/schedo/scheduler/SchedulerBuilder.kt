package org.schedo.scheduler

import org.schedo.controller.TaskController
import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.manager.TaskManager
import org.schedo.repository.*
import org.schedo.repository.inmemory.InMemoryStatus
import org.schedo.repository.inmemory.InMemoryTasks
import org.schedo.repository.postgres.PostgresStatusRepository
import org.schedo.repository.postgres.PostgresTasksRepository
import org.schedo.repository.postgres.createPostgresTables
import org.schedo.waiter.Waiter
import repository.RetryRepository
import repository.postgres.PostgresRetryRepository
import repository.ram.InMemoryRetry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sql.DataSource
import java.time.Duration

private val logger = KotlinLogging.logger {}

class SchedulerBuilder {
    private var executionThreadsCount = Runtime.getRuntime().availableProcessors()

    private var dataSource: DataSource? = null  // null stands for InMemory
    private var dataSourceType: DataSourceType? = null  // null stands for InMemory

    // Waiter configuration
    private var pollingInterval: Duration = Duration.ZERO
    private var busyRatio = 2.7

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

    fun executionThreads(count: Int): SchedulerBuilder {
        executionThreadsCount = count
        return this
    }

    fun pollingInterval(duration: Duration): SchedulerBuilder {
        pollingInterval = duration
        return this
    }

    fun busyRatio(ratio: Double): SchedulerBuilder {
        busyRatio = ratio
        return this
    }

    fun build(): Scheduler {
        val dsType = dataSourceType  // snapshot it

        val tasksRepository: TasksRepository
        val statusRepository: StatusRepository
        val retryRepository: RetryRepository

        when (dsType) {
            null -> {
                val inMemTasks  = InMemoryTasks()
                val inMemStatus = InMemoryStatus()
                tasksRepository = inMemTasks
                statusRepository = inMemStatus
                retryRepository = InMemoryRetry(inMemTasks, inMemStatus)
            }

            is DataSourceType.Postgres -> {
                val pgDataSource = checkNotNull(dataSource) { "Postgres data source is not set" }

                createPostgresTables(pgDataSource)
                tasksRepository = PostgresTasksRepository(pgDataSource)
                statusRepository = PostgresStatusRepository(pgDataSource)
                retryRepository = PostgresRetryRepository(pgDataSource)
            }

            is DataSourceType.Other ->
                error("${dsType.name} is not supported")
        }
        val taskManager = TaskManager(tasksRepository, statusRepository, retryRepository)

        val waiter = Waiter(executionThreadsCount, pollingInterval, busyRatio)
        val executor = Executors.newWorkStealingPool(executionThreadsCount)

        logger.info{"Scheduler configuration:\n" +
                "\tdataSourceType: $dataSourceType\n" +
                "\texecutionThreadsCount: $executionThreadsCount"}

        return Scheduler(taskManager, TaskController(), waiter, executor)
    }
}