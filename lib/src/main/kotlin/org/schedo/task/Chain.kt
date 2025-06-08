package org.schedo.task

import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.SchedulerContext
import java.time.Duration

/**
 * Class for chaining tasks. Note that steps are not copied.
 * Each [andThen], [orElse] or [repeat] invocation adds an
 * action after the given step.
 * @sample exampleTwoAndThen
 */
class Chain private constructor(
    val head: TaskName,
    val tail: Task,
) {
    /**
     * @param stepName should be unique among all tasks
     */
    constructor(stepName: String, retryPolicy: RetryPolicy?, stepBody: Runnable) : this(
        TaskName(stepName),
        object: OneTimeTask(TaskName(stepName), retryPolicy) {
            override fun run() = stepBody.run()
        }
    ) {
        SchedulerContext.taskResolver.addTask(tail)
        // Later tail's handlers may be changed. As taskResolver and
        // tail point to same object, update will be registered
    }



    fun andThen(next: Chain, duration: Duration = Duration.ZERO): Chain {
        tail.successHandlers += { taskManager ->
            // schedule head of next chain
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(next.head, now.plus(duration))
        }
        return Chain(head, next.tail)
    }

    fun orElse(alt: Chain, duration: Duration = Duration.ZERO): Chain {
        tail.failureHandlers += { taskManager ->
            // schedule head of next chain
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(alt.head, now.plus(duration))
        }
        return Chain(head, alt.tail)
    }

    fun repeat(duration: Duration = Duration.ZERO): Chain {
        return this.andThen(this, duration)
    }
}

fun exampleTwoAndThen() {
    val one = Chain("stepOne", null){ println("Step one") }
    val two = Chain("stepTwo", null){ println("Step two") }
    val three = Chain("stepThree", null){ println("Step Three") }
    val oneTwo = one.andThen(two)
    val oneThree = one.andThen(three)
    // After one, steps two and three will be scheduled
}