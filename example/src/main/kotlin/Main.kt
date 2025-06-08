import org.postgresql.ds.PGPoolingDataSource
import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.Scheduler
import org.schedo.scheduler.SchedulerBuilder
import java.time.Duration
import kotlin.random.Random

fun basicExample(scheduler: Scheduler) {
    scheduler.scheduleAfter("one-time", Duration.ofSeconds(8)) {
        println("Hello one-time world")
    }
    scheduler.scheduleRecurring("recurring", Duration.ofSeconds(1)) {
        println("Hello recurring world")
    }
    scheduler.scheduleAfter("looong", Duration.ofSeconds(0)) {
        Thread.sleep(10 * 1000)
        println("Looong task finally completed")
    }

    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
}

fun retryExample(scheduler: Scheduler) {
    // Default exponential backoff is +2s, +4s, +8s, ... (max 3 retry)
    scheduler.scheduleAfter("faulty", Duration.ofMillis(10), RetryPolicy.ExpBackoff()) {
        println("Hello one-time world")
        throw RuntimeException("Ooops")
    }
    scheduler.scheduleAfter("normal", Duration.ofSeconds(5), RetryPolicy.ExpBackoff()) {
        println("Non-faulty")
    }

    scheduler.start()
    Thread.sleep(20 * 1000)
    scheduler.stop()
    // Expected behaviour: Hello one-time world will be written 4 times
    // At moments: right after start, in 2s, in (2+4)s, in (2+4+8)s
}

fun cronExample(scheduler: Scheduler) {
    scheduler.scheduleRecurring("CronRecurring", "*/2 * * * * ?"){
        println("Hello cron world")
    }

    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
}

fun serverExample(scheduler: Scheduler) {
    scheduler.scheduleRecurring("task1", Duration.ofSeconds(2),
        RetryPolicy.FixedDelay(3u, Duration.ofSeconds(2))) {
        println("task1")
        if (Random.nextInt(0, 3) == 0) {
            throw RuntimeException("Task1 failed")
        }
    }
    scheduler.scheduleRecurring("task2", Duration.ofSeconds(1),
        RetryPolicy.FixedDelay(3u, Duration.ofSeconds(2))) {
        println("task2")
        if (Random.nextInt(0, 4) == 0) {
            throw RuntimeException("Task2 failed")
        }
    }
    scheduler.scheduleAfter("task3", Duration.ofSeconds(30)) {
        println("task3 completed")
    }

    scheduler.start(true)
//    Thread.sleep(40 * 1000)
//    scheduler.stop()
}

fun main() {
    val source: PGPoolingDataSource = PGPoolingDataSource()
        .apply {
            dataSourceName = "A Data Source"
            serverName = "localhost:15432"
            databaseName = "app_db"
            user = "app_user"
            password = "app_password"
            maxConnections = 10
        }

    // Scheduler settings
    val scheduler = SchedulerBuilder()
       // .dataSource(source)
        .launchServer()
        .build()

    // Task scheduling
    scheduler.scheduleAfter("Greeter", Duration.ofSeconds(40)) {
        println("Hello world!")
    }
    scheduler.scheduleRecurring("Intrusive", "*/2 * * * * ?") {
        println("I'm here!")
    }
    scheduler.scheduleRecurring("Rare", Duration.ofMinutes(1)) {
        println("Once a minute")
    }
    scheduler.scheduleRecurring("Looong", Duration.ofSeconds(10)) {
        Thread.sleep(Duration.ofSeconds(20))
    }
    scheduler.scheduleRecurring("Faulty", Duration.ofSeconds(3),
        RetryPolicy.FixedDelay(3u, Duration.ofSeconds(1)))
    {
        if (Random.nextInt(0, 2) == 0) {
            println("Faulty failed")
            throw RuntimeException("Unexpected error")
        } else {
            println("Faulty completed, next repeat in 3s")
        }
    }

    scheduler.start()
//    Thread.sleep(50 * 1000)
//    scheduler.stop()
}