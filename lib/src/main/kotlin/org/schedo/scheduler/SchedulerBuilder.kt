package org.schedo.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.schedo.server.TaskController
import org.schedo.manager.TaskManager
import org.schedo.manager.TaskResolver
import org.schedo.repository.*
import org.schedo.repository.inmemory.*
import org.schedo.repository.postgres.*
import org.schedo.server.SchedoServer
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import org.schedo.waiter.Waiter
import java.util.concurrent.Executors
import javax.sql.DataSource
import java.time.Duration

private val logger = KotlinLogging.logger {}

class SchedulerBuilder {
    private var executionThreadsCount = Runtime.getRuntime().availableProcessors()

    private var dataSource: DataSource? = null  // null stands for InMemory
    private var dataSourceType: DataSourceType? = null  // null stands for InMemory
    private var serverPort = 8080
    private var launchServer = false

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

    fun launchServer(): SchedulerBuilder {
        launchServer = true
        return this
    }

    fun serverPort(port: Int): SchedulerBuilder {
        this.serverPort = port
        return this
    }

    fun build(): Scheduler {
        val dsType = dataSourceType  // snapshot it

        val tasksRepository: TasksRepository
        val statusRepository: StatusRepository
        val executionsRepository: ExecutionsRepository
        val transactionManager: TransactionManager

        when (dsType) {
            null -> {
                val inMemoryJoin = InMemoryJoin()
                tasksRepository = InMemoryTasks(inMemoryJoin)
                statusRepository = InMemoryStatus(inMemoryJoin)
                executionsRepository = InMemoryExecutions()

                transactionManager = InMemoryTransaction()
            }

            is DataSourceType.Postgres -> {
                val pgDataSource = checkNotNull(dataSource) { "Postgres data source is not set" }

                createPostgresTables(pgDataSource)
                transactionManager = DataSourceTransaction(pgDataSource)

                tasksRepository = PostgresTasksRepository(transactionManager)
                statusRepository = PostgresStatusRepository(transactionManager)
                executionsRepository = PostgresExecutionsRepository(transactionManager)
            }

            is DataSourceType.Other ->
                error("${dsType.name} is not supported")
        }

        val dateTimeService: DateTimeService = DefaultDateTimeService()
        val taskManager = TaskManager(tasksRepository, statusRepository, executionsRepository,
            transactionManager, TaskResolver(), dateTimeService)

        var server: SchedoServer? = null
        if (launchServer) {
            val taskController = TaskController(tasksRepository, statusRepository,
                executionsRepository, transactionManager, taskManager)
            server = SchedoServer(serverPort, taskController)
        }

        val waiter = Waiter(executionThreadsCount, pollingInterval, busyRatio)
        val executor = Executors.newWorkStealingPool(executionThreadsCount)

        logger.info{"Scheduler configuration:\n" +
                "\tdataSourceType: $dataSourceType\n" +
                "\texecutionThreadsCount: $executionThreadsCount"}

        return Scheduler(taskManager, server, waiter, executor, dateTimeService)
    }
}