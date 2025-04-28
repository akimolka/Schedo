package components

import task.Task
import task.TaskName
import java.util.concurrent.ConcurrentHashMap

class TaskResolver() {
    private val mapping = ConcurrentHashMap<TaskName, Task>()

    fun addTask(task: Task) {
        mapping[task.name] = task
    }

    fun getTask(name: TaskName): Task? {
        return mapping[name]
    }
}