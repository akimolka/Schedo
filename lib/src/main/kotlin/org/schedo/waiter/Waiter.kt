package org.schedo.waiter

import org.schedo.util.DateTimeService
import org.schedo.util.DefaultDateTimeService
import java.lang.Thread.sleep
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread that picks tasks from the taskRepository should wait in two scenarios:
 * 1) There are no tasks to execute - wake up with polling interval [duration]
 * 2) All executor threads are busy - should be woken up by executor when most of the tasks are completed
 * The second also guarantees even workload on different servers
 */
class Waiter(
    private val threadsCount: Int,
    private val pollingInterval: Duration = Duration.ZERO,
    private val busyRatio: Double = 2.7,
) {
    private val monitor = Object()
    @Volatile
    private var busy: Boolean = false

    fun isBusy(executions: AtomicInteger): Boolean {
        return executions.toDouble() / threadsCount > busyRatio
    }

    fun sleepPollingInterval() {
        sleep(pollingInterval.toMillis())
    }

    fun waitLightLoad(executions: AtomicInteger) {
        // May have missed wakePoller from executor => check isBusy(executions) once again
        synchronized(monitor) {
            busy = true
            if (isBusy(executions)) {
                while (busy) {
                    monitor.wait()
                }
            } else {
                busy = false
            }
        }
    }

    /**
     * busy = true <-> maybe sleeping
     * busy = false <-> surely awake
     */
    fun wakePoller() {
        if (busy) {
            // avoid synchronized if possible
            synchronized(monitor) {
                busy = false
                monitor.notify()
            }
            println()
        }
    }
}