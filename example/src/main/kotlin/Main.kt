import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource
import java.sql.DriverManager;

import scheduler.Scheduler
import scheduler.SchedulerBuilder
import java.time.Duration

fun dataPGDataSource(pgUrl: String, pgUser: String, pgPassword: String): DataSource = PGSimpleDataSource().apply {
    setUrl(pgUrl)
    user = pgUser
    password = pgPassword
}

fun main() {
    val pgUrl = "jdbc:postgresql://localhost:15432/app_db"
    val pgUser = "app_user"
    val pgPassword = "app_password"
    val conn = DriverManager.getConnection(pgUrl, pgUser, pgPassword)

    val scheduler = SchedulerBuilder()
        .setRepository(repository.JDBCRepository(conn))
        .build()
    scheduler.scheduleAfter(Duration.ofSeconds(5)){
        println("Hello one-time world")
    }
    scheduler.scheduleRecurring(Duration.ofSeconds(1)){
        println("Hello recurring world")
    }


    scheduler.start()
    scheduler.scheduleAfter("PrintsABC", Duration.ofSeconds(3)){
        println("ABC")
    }

    Thread.sleep(10 * 1000)
    scheduler.stop()
}