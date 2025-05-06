package org.schedo.task

import org.schedo.manager.TaskManager

class OnFailedContext(val e: Exception, val failedCount: Int, val taskManager: TaskManager)