import Scheduler
import java.time.Duration


fun main() {
    val scheduler = Scheduler()
    scheduler.scheduleAfter(Duration.ofSeconds(5)){
        println("Hello one-time world")
    }
    scheduler.scheduleRecurring(Duration.ofSeconds(5)){
        println("Hello recurring world")
    }
    scheduler.run()
}