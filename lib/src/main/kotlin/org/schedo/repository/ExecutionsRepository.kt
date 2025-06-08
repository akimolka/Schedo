package org.schedo.repository

import org.schedo.task.TaskName

interface ExecutionsRepository {
    fun setRetryCount(task: TaskName, value: Int)
    fun resetRetryCount(task: TaskName) = setRetryCount(task, 0)
    fun addRetryCount(task: TaskName, delta: Int)
    fun increaseRetryCount(task: TaskName) = addRetryCount(task, 1)
    fun getRetryCount(task: TaskName): UInt
}