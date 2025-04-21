import kotlin.time.TimeMark

class ScheduledTask(val func: () -> Unit, val executionTime: TimeMark)