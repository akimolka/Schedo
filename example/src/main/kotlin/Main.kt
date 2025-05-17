import org.postgresql.ds.PGPoolingDataSource
import org.schedo.retry.RetryPolicy
import org.schedo.scheduler.Scheduler
import org.schedo.scheduler.SchedulerBuilder
import org.schedo.task.SequenceBuilder
import org.schedo.task.TaskBuilder
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

fun cronExample(scheduler: Scheduler) {
    scheduler.scheduleRecurring("CronRecurring", "*/2 * * * * ?"){
        println("Hello cron world")
    }

    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
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
        .build()

    val stepOne = TaskBuilder("stepOne"){
        val r = Random.nextInt(0, 3)
        if (r == 0) {
            println("Step One Failed")
            throw RuntimeException("Step one Failed")
        } else {
            println("Step One")
        }
    }.retryPolicy(RetryPolicy.FixedDelay(3u, Duration.ofSeconds(1)))
        .build()

    val stepTwo = TaskBuilder("stepTwo"){
        println("Step Two")
    }.build()

    val stepThree = TaskBuilder("stepThree"){
        println("Step Three")
    }.build()

    val oneTwo = SequenceBuilder("oneTwo", stepOne).andThen(stepTwo).build()
    val oneTwoThree = SequenceBuilder("oneTwoThree", stepOne).andThen(stepTwo).andThen(stepThree).build()

    scheduler.scheduleRecurring(oneTwo, Duration.ofSeconds(1))
    //scheduler.scheduleRecurring(oneTwoThree, Duration.ofSeconds(1))

    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
}