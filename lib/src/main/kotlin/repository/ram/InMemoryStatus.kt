package repository.ram

import repository.StatusRepository
import repository.TaskResult
import task.TaskName

class InMemoryStatus : StatusRepository{
    override fun schedule(name: TaskName) {}
    override fun enqueue(name: TaskName) {}
    override fun start(name: TaskName) {}
    override fun finish(name: TaskName, result: TaskResult) {}
}