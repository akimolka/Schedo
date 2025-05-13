package org.schedo.server
import org.schedo.repository.ScheduledTaskInstance
import org.schedo.repository.StatusRepository
import org.schedo.repository.TasksRepository
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime

class TaskController(
    private val tasksRepository: TasksRepository,
    private val statusRepository: StatusRepository,
) {
    fun countTasksDue(moment: OffsetDateTime): Int {
        return tasksRepository.countTaskInstancesDue(moment)
    }

    fun scheduledTasks(): List<ScheduledTaskInstance> {
        return emptyList()
    }

    fun finishedTasks(): List<TaskInstanceID /*TaskName, finishedAt, additional info*/> {
        // In additional info include time of execution: finishedAt - startedAt
        // And error if failed: add error field to Status Table?
        return emptyList()
    }

    fun rescheduleTask(task: TaskName) {
        // Button to reschedule finished task
    }

    fun cancelTask(task: TaskName) {
        // no such feature yet
    }
}