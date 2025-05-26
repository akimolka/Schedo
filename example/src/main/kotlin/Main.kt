import org.postgresql.ds.PGPoolingDataSource
import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.Scheduler
import org.schedo.scheduler.SchedulerBuilder
import java.lang.Thread.sleep
import java.time.Duration

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
    scheduler.scheduleAfter("faulty", Duration.ofSeconds(0), RetryPolicy.ExpBackoff()) {
        println("Hello one-time world")
        throw RuntimeException("Ooops")
    }

    scheduler.start()
    Thread.sleep(20 * 1000)
    scheduler.stop()
    // Expected behaviour: Hello one-time world will be written 4 times
    // At moments: right after star, in 2s, in (2+4)s, in (2+4+8)s
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

    val scheduler = SchedulerBuilder()
        //.dataSource(source)
        .executionThreads(2)
        .pollingInterval(Duration.ofMillis(100))
        .build()

    scheduler.scheduleRecurring("CronRecurring", "*/2 * * * * ?"){
        println("Hello cron world")
    }
    for (i in 1..10) {
        val millis = 10 * i.toLong()
        for (j in 1..10) {
            scheduler.scheduleRecurring("recurring$i/$j", Duration.ofMillis(millis)){
                sleep(millis)
            }
        }
    }

    scheduler.start()
    Thread.sleep(5 * 1000)
    scheduler.stop()
}