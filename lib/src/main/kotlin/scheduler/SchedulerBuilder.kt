package scheduler

import repository.InMemoryRepository
import repository.Repository
import java.util.concurrent.Executors

class SchedulerBuilder {
    private var repository: Repository = InMemoryRepository()
    private var executor = Executors.newCachedThreadPool()

    fun setRepository(repo: Repository): SchedulerBuilder {
        repository = repo
        return this
    }

    fun build(): Scheduler {
        return Scheduler(repository, executor)
    }
}