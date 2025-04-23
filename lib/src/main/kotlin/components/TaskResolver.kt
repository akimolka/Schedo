package components

import task.ScheduledTask

class TaskResolver() {
    private val mapping = mutableMapOf<String, ScheduledTask>()
    fun addTask(task: ScheduledTask) {
        mapping[task.name] = task
    }
    fun getTask(name: String): ScheduledTask? {
        return mapping[name]
    }
}