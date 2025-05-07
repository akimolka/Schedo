package org.schedo.controller
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.engine.*
import org.schedo.repository.ScheduledTaskInstance
import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName

class TaskController {
    // хранит ссылки на все репозитории? (не хочется дублировать)
    // имеет методы нужные для апи (какие?)

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

    fun run() {
        embeddedServer(Netty, 8080) {
            // здесь для каждой ручки вызывает соотсветствующий метод
            routing {
                get("/scheduledTasks") {
                    call.respondText("List of scheduled tasks", ContentType.Text.Html)
                }
                get("/") {
                    call.respondText("Hello, world!", ContentType.Text.Html)
                }
            }
        }.start()
    }
}