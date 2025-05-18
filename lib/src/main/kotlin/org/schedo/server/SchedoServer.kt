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
import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.Duration

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
            routing {
                get("/scheduled/count") {
                    val due = call.parseDue(dateTimeService) ?: return@get
                    val count = taskController.countScheduledTasks(due)
                    call.respondText(
                        count.toString(),
                        ContentType.Text.Plain,
                        HttpStatusCode.OK
                    )
                }
                get("/scheduled") {
                    val due = call.parseDue(dateTimeService) ?: return@get
                    call.respond(taskController.scheduledTasks(due))
                }
                get("/tasks") {

                }
                get("/") {
                    call.respondText("Hello, world!", ContentType.Text.Html)
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
}