package org.schedo.repository.inmemory

import org.schedo.repository.ExecutionsRepository
import org.schedo.task.TaskName

class InMemoryExecutions : ExecutionsRepository {
    override fun setRetryCount(task: TaskName, value: Int) {
        TODO("Not yet implemented")
    }

    override fun addRetryCount(task: TaskName, delta: Int) {
        TODO("Not yet implemented")
    }

    override fun getRetryCount(task: TaskName): UInt {
        TODO("Not yet implemented")
    }

}