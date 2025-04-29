import org.postgresql.ds.PGPoolingDataSource
import org.postgresql.ds.PGSimpleDataSource
import scheduler.SchedulerBuilder
import java.sql.DriverManager
import java.time.Duration
import javax.sql.ConnectionPoolDataSource
import javax.sql.DataSource
import repository.RepositoryType

fun dataPGDataSource(pgUrl: String, pgUser: String, pgPassword: String): DataSource = PGSimpleDataSource().apply {
    setUrl(pgUrl)
    user = pgUser
    password = pgPassword
}

fun main() {
    val source: PGPoolingDataSource = PGPoolingDataSource()
    source.setDataSourceName("A Data Source")
    source.serverName = "localhost:15432"

    source.setDatabaseName("app_db")
    source.setUser("app_user")
    source.setPassword("app_password")
    source.setMaxConnections(10)

//    val source: PGPoolingDataSource = PGPoolingDataSource()
//    source.setDataSourceName("A Data Source")
//    source.serverName = "localhost:15432"
//
//    source.setDatabaseName("test")
//    source.setUser("testuser")
//    source.setPassword("testpassword")
//    source.setMaxConnections(10)

    val scheduler = SchedulerBuilder()
        .setRepository(RepositoryType.Postgres(source))
        .build()
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