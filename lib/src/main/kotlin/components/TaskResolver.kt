package components

import task.ScheduledTask
import task.TaskName
import java.util.concurrent.ConcurrentHashMap

class TaskResolver() {
    private val mapping = ConcurrentHashMap<TaskName, ScheduledTask>()

    fun addTask(task: ScheduledTask) {
        mapping[task.name] = task
    }

    fun getTask(name: TaskName): ScheduledTask? {
        return mapping[name]
    }
}