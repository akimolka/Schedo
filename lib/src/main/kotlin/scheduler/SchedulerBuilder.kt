package scheduler

import repository.*
import java.util.concurrent.Executors

class SchedulerBuilder {
    private var repository: Repository = Repository(RepositoryType.InMemory)
    private var executor = Executors.newCachedThreadPool()

    fun setRepository(repositoryType: RepositoryType): SchedulerBuilder {
        repository = Repository(repositoryType)
        return this
    }

    fun build(): Scheduler {
        return Scheduler(repository, executor)
    }
}