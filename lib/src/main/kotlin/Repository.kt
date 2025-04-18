import task.ScheduledTask

class InMemoryRepository {
    private var tasks = mutableListOf<ScheduledTask>()

    fun add(task: ScheduledTask) {
        tasks.add(task)
    }

    fun pickDueExecution(): List<ScheduledTask> {
        val (picked, rest) = tasks.partition { it.executionTime.hasPassedNow() }
        tasks = rest.toMutableList()
        return picked
    }
}