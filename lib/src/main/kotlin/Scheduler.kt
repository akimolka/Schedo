import kotlin.time.Duration
import kotlin.collections.mutableListOf
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Scheduler {
    val timeSource = TimeSource.Monotonic
    var tasks = mutableListOf<Pair<() -> Unit, TimeMark>>()
    fun scheduleAfter(duration: Duration, func: () -> Unit) {
        tasks.add(func to timeSource.markNow() + duration)
    }
    fun run() {
        while (true) {
            val (tasksToRun, rest) = tasks.partition { it.second.hasPassedNow() }
            tasks = rest.toMutableList()
            for (task in tasksToRun) {
                task.first()
            }
        }
    }
}