package org.schedo.repository

import org.schedo.task.TaskInstanceID
import org.schedo.task.TaskName
import java.time.OffsetDateTime

enum class TaskStatus {
    RESUMED, RUNNING, FINISHED, CANCELLED
}

interface ExecutionsRepository {
    fun setRetryCount(task: TaskName, value: Int)
    fun resetRetryCount(task: TaskName) = setRetryCount(task, 0)
    fun addRetryCount(task: TaskName, delta: Int)
    fun increaseRetryCount(task: TaskName) = addRetryCount(task, 1)
    fun getRetryCount(task: TaskName): UInt

    /**
     * Updates Task status. If update marks Task end (status = COMPLETED or FINISHED) and
     * currentInstance in SchedoExecutions does not match instanceID, update
     * is considered outdated and is not applied.
     */
    fun updateStatus(task: TaskName, instanceID: TaskInstanceID, status: TaskStatus)

    /**
     * Cancels future executions of the given task.
     * @return Whether the action changed the cancelled state.
     * False if task was already cancelled
     */
    fun cancel(task: TaskName, moment: OffsetDateTime): Boolean
    /**
     * Clears flag 'cancelled'.
     * @return Whether the action changed the cancelled state.
     * False if task was not cancelled.
     */
    fun clearCancelled(task: TaskName): Boolean
    fun whenCancelled(task: TaskName): OffsetDateTime?

    /** @return whether action was successful
    */
    fun tryResume(task: TaskName): Boolean

    fun getStatusAndCancelled(task: TaskName): Pair<TaskStatus, Boolean>?
}