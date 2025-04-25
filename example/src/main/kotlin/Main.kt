import org.postgresql.ds.PGPoolingDataSource
import org.postgresql.ds.PGSimpleDataSource
import scheduler.SchedulerBuilder
import java.sql.DriverManager
import java.time.Duration
import javax.sql.DataSource


fun dataPGDataSource(pgUrl: String, pgUser: String, pgPassword: String): DataSource = PGSimpleDataSource().apply {
    setUrl(pgUrl)
    user = pgUser
    password = pgPassword
}

fun main() {
    val pgUrl = "jdbc:postgresql://localhost:15432/app_db"
    val pgUser = "app_user"
    val pgPassword = "app_password"
//    val conn = DriverManager.getConnection(pgUrl, pgUser, pgPassword)


//    val source: PGPoolingDataSource = PGPoolingDataSource()
//    source.setDataSourceName("A Data Source")
//    source.serverName = "localhost:15432"
//
//    source.setDatabaseName("test")
//    source.setUser("testuser")
//    source.setPassword("testpassword")
//    source.setMaxConnections(10)

    val scheduler = SchedulerBuilder()
//        .setRepository(repository.PostgresRepository(source))
        .build()
    scheduler.scheduleAfter("test1", Duration.ofSeconds(5)) {
        println("Hello one-time world")
    }
    scheduler.scheduleRecurring("test2", Duration.ofSeconds(1)) {
        println("Hello recurring world")
    }


    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
}