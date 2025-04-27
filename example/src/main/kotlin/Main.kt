import scheduler.Scheduler
import java.time.Duration


fun main() {
    val scheduler = Scheduler()
    scheduler.scheduleAfter(Duration.ofSeconds(5)){
        println("Hello one-time world")
    }
    scheduler.scheduleRecurring(Duration.ofSeconds(1)){
        println("Hello recurring world")
    }


    scheduler.start()
    Thread.sleep(10 * 1000)
    scheduler.stop()
}