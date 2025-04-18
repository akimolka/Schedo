import task.ScheduledTask

class InMemoryRepository {
    @Volatile private var tasks = listOf<ScheduledTask>()
    var counter = 1

    fun add(task: ScheduledTask) {
        tasks = tasks + task
        counter += 1
    }

    fun pickDueExecution(): List<ScheduledTask> { // передавать время
        val (picked, rest) = tasks.partition { it.executionTime.hasPassedNow() }
        println(counter)
        tasks = rest
        return picked
    }
}