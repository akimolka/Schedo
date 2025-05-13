package org.schedo.server

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class SchedoServer(private val taskController: TaskController) {
    fun run() {
        embeddedServer(Netty, 8080) {
            // здесь для каждой ручки вызывает соотсветствующий метод
            routing {
                get("/countScheduledTasksDue") {
                    val momentParam = call.request.queryParameters["moment"]
                    val moment = if (momentParam == null) {
                        OffsetDateTime.now()
                    } else {
                        try {
                            OffsetDateTime.parse(momentParam)
                        } catch (e: DateTimeParseException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Invalid 'moment' format: must be ISO-8601," +
                                        " e.g. ?moment=2025-05-13T15:30:00Z, got '$momentParam'"
                            )
                            return@get
                        }
                    }

                    val count = taskController.countTasksDue(moment)
                    call.respondText(
                        count.toString(),
                        ContentType.Text.Plain,
                        HttpStatusCode.OK
                    )
                }
                get("/listScheduledTasks") {
                    call.respondText("List of scheduled tasks", ContentType.Text.Html)
                }
                get("/") {
                    call.respondText("Hello, world!", ContentType.Text.Html)
                }
            }
        }.start()
    }
}