package org.schedo.task

import org.schedo.manager.TaskManager
import org.schedo.manager.TaskResolver
import org.schedo.retry.RetryPolicy
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Class for chaining tasks. Note that steps are not copied.
 * Each [andThen], [orElse] or [repeat] invocation adds an
 * action after the given step.
 * @sample exampleTwoAndThen
 */
class Chain private constructor(
    val head: TaskName,
    val tail: Task,
    val adjacent: MutableList<Chain> = mutableListOf(),
) {
    /**
     * @param stepName should be unique among all tasks
     */
    constructor(stepName: String, retryPolicy: RetryPolicy?, stepBody: Runnable) : this(
        TaskName(stepName),
        object: OneTimeTask(TaskName(stepName), retryPolicy) {
            override fun run() = stepBody.run()
        }
    )

    fun andThen(next: Chain, duration: Duration = Duration.ZERO): Chain {
        tail.successHandlers += { taskManager ->
            // schedule head of next chain
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(next.head, now.plus(duration))
        }
        val res = Chain(head, next.tail)
        res.addLink(this, next)
        return res
    }

    fun orElse(alt: Chain, duration: Duration = Duration.ZERO): Chain {
        tail.failureHandlers += { taskManager ->
            // schedule head of next chain
            val now = taskManager.dateTimeService.now()
            taskManager.schedule(alt.head, now.plus(duration))
        }
        val res = Chain(head, alt.tail)
        res.addLink(this, alt)
        return res
    }

    fun repeat(duration: Duration = Duration.ZERO): Chain {
        return this.andThen(this, duration)
    }

    private fun addLink(left: Chain, right: Chain) {
        left.adjacent += this
        adjacent += left
        adjacent += right
    }

    fun addToTaskResolver(taskResolver: TaskResolver) {
        val tasksAdded: MutableSet<Task> = mutableSetOf()
        val chainsVisited: MutableSet<Chain> = mutableSetOf()
        dfs(taskResolver, tasksAdded, chainsVisited)
    }

    private fun dfs(taskResolver: TaskResolver, tasksAdded: MutableSet<Task>, chainsVisited: MutableSet<Chain>) {
        chainsVisited += this
        if (tail !in tasksAdded) {
            taskResolver.addTask(tail)
            tasksAdded += tail
        }
        adjacent.forEach{ if (it !in chainsVisited) it.dfs(taskResolver, tasksAdded, chainsVisited) }
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