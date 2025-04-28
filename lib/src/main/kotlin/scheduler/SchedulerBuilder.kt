package scheduler

import repository.InMemoryRepository
import repository.TasksRepository
import java.util.concurrent.Executors

class SchedulerBuilder {
    private var repository: TasksRepository = InMemoryRepository()
    private var executor = Executors.newCachedThreadPool()

    fun setRepository(repo: TasksRepository): SchedulerBuilder {
        repository = repo
        return this
    }

    fun build(): Scheduler {
        return Scheduler(repository, executor)
    }
}