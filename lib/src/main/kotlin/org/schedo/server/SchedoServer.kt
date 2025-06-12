package org.schedo.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import org.schedo.repository.Status
import org.schedo.repository.StatusEntry
import org.schedo.task.TaskName
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.Duration
import io.ktor.server.plugins.cors.routing.CORS

class SchedoServer(
    private val port: Int,
    private val taskController: TaskController,
    private val dateTimeService: DateTimeService = DefaultDateTimeService(),
) {
    fun run() {
        embeddedServer(Netty, 8080) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(CORS) {
                anyHost()
                anyMethod()
            }
            routing {
                get("/tasks/scheduled/count") {
                    val due = call.parseDue(dateTimeService) ?: return@get
                    val count = taskController.countScheduledTasks(due)
                    call.respondText(
                        count.toString(),
                        ContentType.Text.Plain,
                        HttpStatusCode.OK
                    )
                }
                get("/tasks/scheduled") {
                    val due = call.parseDue(dateTimeService) ?: return@get
                    call.respond(taskController.scheduledTasks(due))
                }
                get("/tasks/failed") {
                    call.respond(taskController.failedTasks())
                }
                get("/tasks/{taskName}") {
                    // TODO What if there is a task "failed"?
                    val nameParam = call.parameters["taskName"]
                    if (nameParam == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing path parameter 'taskName'"
                        )
                        return@get
                    }

                    val detailedTaskInfo = taskController.taskHistory(TaskName(nameParam))

                    if (detailedTaskInfo.history.isEmpty()) {
                        // TODO guarantee that two other field are not empty
                        call.respond(
                            HttpStatusCode.NotFound,
                            "No task named '$nameParam'"
                        )
                        return@get
                    }

                    call.respond(detailedTaskInfo)
                }
                get("/tasks") {
                    val from = call.parseTime("from", OffsetDateTime.MIN) ?: return@get
                    val to = call.parseTime("to", OffsetDateTime.MAX) ?: return@get
                    val taskName = call.parameters["taskName"]?.let{ TaskName(it) }
                    val status = call.parameters["status"]?.let {call.parseStatus(it) ?: return@get}

                    call.respond(taskController.tasks(from, to, taskName, status))
                }
                get("/") {
                    call.respondText("Server is healthy", ContentType.Text.Html)
                }

                post("/tasks/{taskName}/cancel") {
                    val nameParam = call.parameters["taskName"]
                    if (nameParam == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing path parameter 'taskName'"
                        )
                        return@post
                    }

                    val success = taskController.cancelTask(TaskName(nameParam))

                    if (success) {
                        call.respond(HttpStatusCode.OK, "Cancelled")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Task is already finished or cancelled")
                    }
                }

                post("/tasks/{taskName}/resume") {
                    val nameParam = call.parameters["taskName"]
                    if (nameParam == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing path parameter 'taskName'"
                        )
                        return@post
                    }

                    val success = taskController.resumeTask(TaskName(nameParam))

                    if (success) {
                        call.respond(HttpStatusCode.OK, "Resumed")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Task is already running or resumed")
                    }
                }
            }
        }.start()
    }

    private suspend fun ApplicationCall.parseDue(dateTimeService: DateTimeService): OffsetDateTime? {
        val raw = request.queryParameters["duration"] ?: return OffsetDateTime.MAX
        return try {
            dateTimeService.now().plus(Duration.parse(raw))
        } catch (e: DateTimeParseException) {
            respond(
                HttpStatusCode.BadRequest,
                "Invalid 'duration' format: must be ISO-8601, e.g. PT15M or P1DT2H, got '$raw'"
            )
            null
        }
    }

    private suspend fun ApplicationCall.parseTime(param: String, default: OffsetDateTime): OffsetDateTime? {
        val raw = request.queryParameters[param] ?: return default
        return try {
            OffsetDateTime.parse(raw)
        } catch (e: DateTimeParseException) {
            respond(
                HttpStatusCode.BadRequest,
                "Invalid '$param' format: must be ISO-8601, e.g. 2025-05-13T15:30:00Z, got '$raw'"
            )
            null
        }
    }

    private suspend fun ApplicationCall.parseStatus(raw: String): Status? {
        return try {
            Status.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            respond(
                HttpStatusCode.BadRequest,
                "Invalid 'status' parameter: must be one of ${Status.entries.joinToString()}"
            )
            null
        }
    }
}