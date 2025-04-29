import org.postgresql.ds.PGPoolingDataSource
import org.postgresql.ds.PGSimpleDataSource
import org.schedo.scheduler.SchedulerBuilder
import java.sql.DriverManager
import java.time.Duration
import javax.sql.ConnectionPoolDataSource
import javax.sql.DataSource

fun main() {
    val source: PGPoolingDataSource = PGPoolingDataSource()
        .apply {
            dataSourceName = "A Data Source"
            serverName = "localhost:15432"
            databaseName = "app_db"
            user = "app_db"
            password = "app_password"
            maxConnections = 10
        }

    val scheduler = SchedulerBuilder()
        //.dataSource(source)
        .build()
//    scheduler.scheduleAfter("one-time", Duration.ofSeconds(8)) {
//        println("Hello one-time world")
//    }
//    scheduler.scheduleRecurring("recurring", Duration.ofSeconds(1)) {
//        println("Hello recurring world")
//    }
//    scheduler.scheduleAfter("looong", Duration.ofSeconds(0)) {
//        Thread.sleep(10 * 1000)
//        println("Looong task finally completed")
//    }

    // Default exponential backoff is +2s, +4s, +8s, ... (max 3 retry)
    scheduler.scheduleAfter("faulty", Duration.ofSeconds(0)) {
        println("Hello one-time world")
        throw RuntimeException("Ooops")
    }

    scheduler.start()
    Thread.sleep(20 * 1000)
    scheduler.stop()
}