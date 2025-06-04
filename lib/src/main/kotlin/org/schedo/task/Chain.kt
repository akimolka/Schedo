package org.schedo.task

import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.SchedulerContext
import java.time.Duration

/**
 * Class for chaining tasks. Note that steps are not copied.
 * Each step may have at most one continuation for success and failure (see [successBranchOverwrite]).
 * One can set continuation for both success and failure (see [bothBranches]).
 * @sample successBranchOverwrite
 * @sample bothBranches
 */
class Chain(
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
        tail.successHandler = { taskManager ->
            // schedule head of next chain
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(next.head, now.plus(duration))
        }
        return Chain(head, next.tail)
    }

    fun orElse(alt: Chain, duration: Duration = Duration.ZERO): Chain {
        tail.failureHandler = { taskManager ->
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

fun successBranchOverwrite() {
    val one = Chain("stepOne", null){ println("Step one") }
    val two = Chain("stepTwo", null){ println("Step two") }
    val three = Chain("stepThree", null){ println("Step Three") }
    val oneTwo = one.andThen(two)
    val oneThree = one.andThen(three) // Link one -> two was substituted by one -> three
}

fun bothBranches() {
    val one = Chain("stepOne", null){ /* Faulty */ }
    val two = Chain("stepTwo", null){ println("After successful step one") }
    val three = Chain("stepThree", null){ println("After failed step one") }
    val successBranch = one.andThen(two)
    val failureBranch = one.orElse(three)
    // schedule either one of successBranch and failureBranch
}