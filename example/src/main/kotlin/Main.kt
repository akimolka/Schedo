import Scheduler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
    val scheduler = Scheduler()
//    scheduler.scheduleAfter(10.seconds){
//        println("Hello one-time world")
//    }
    scheduler.scheduleRecurring(1.seconds){
        println("Hello recurring world")
    }
    scheduler.run()
}