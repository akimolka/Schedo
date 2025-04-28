package repository

import task.TaskName
import java.time.Duration

sealed interface TaskResult {
    class Success(val spendingTime: Duration) : TaskResult
    class Failed(val e: Exception) : TaskResult
}

interface StatusRepository {
    fun schedule(name: TaskName)
    fun start(name: TaskName)
    fun finish(name: TaskName, result: TaskResult)
}