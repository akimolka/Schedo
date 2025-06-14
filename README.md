# Schedo
## Kotlin cluster-friendly task scheduler library


### Example of a program
The scheduler interface consists of the following parts:
1) [scheduler setting](#scheduler-setting)
2) [task scheduling](#task-scheduling)
3) scheduler start and stop
```kotlin
val scheduler = SchedulerBuilder()
    .dataSource(source)
    .launchServer()
    .executionThreads(2)
    .build()

scheduler.scheduleAfter("one-time", Duration.ofSeconds(8)) {
    println("Hello one-time world")
}
scheduler.scheduleRecurring("recurring", Duration.ofSeconds(1)) {
    println("Hello recurring world")
}

scheduler.start()
Thread.sleep(50 * 1000)
scheduler.stop()  // optional
```
Tasks can be scheduled both before and after `scheduler.start()`
```kotlin
val scheduler = SchedulerBuilder()
    .build()

scheduler.scheduleAfter("one-time", Duration.ofSeconds(8)) {
    println("Hello one-time world")
}

scheduler.start()

scheduler.scheduleRecurring("recurring", Duration.ofSeconds(1)) {
    println("Hello recurring world")
}
```


### Scheduler setting
Use `SchedulerBuilder()` to tune settings and obtain `Scheduler` object
```kotlin
val scheduler = SchedulerBuilder()
    .dataSource(source)
    .launchServer()
    .serverPort(8080)
    .executionThreads(4)
    .pollingInterval(Duration.ofSeconds(10))
    .busyRatio(3.0)
    .build()
```
- `.dataSource(source)` - Schedo provides two types of storages: inMemory (default) and Postgres. 
To choose Postgres, pass `javax.sql.DataSource` via `.dataSource(source)`.
Schedo servers connected to the same db work mutually with the guarantee that each task will be executed only once.
- `.launchServer()` - Server listening to HTTP routes will be launched after `scheduler.start()`. Server must be launched for
the dashboard to work. Use `.serverPort(Int)` to specify port.
- `.executionThreads(Int)` - Sets numbers of threads in thread pool. 'Runtime.getRuntime().availableProcessors() is default'
- `.pollingInterval(Duration)` - Once in this period scheduler checks `TasksRepository` for ready to be executed tasks.
  Duration.ZERO, aka spin loop, is default.
- `.busyRatio(Double)` - If tasks in executor per thread exceeds this value, scheduler suspends its execution until ratio
goes down to `busyRatio`


### Task scheduling
All tasks **must have unique names**. 

However, to run multiple servers, one can launch several copies of the same program. 
Schedo handles this case and guarantees that each task will be executed only once, despite the fact that it is 
actually scheduled multiple times (once from each server).

For simple tasks use methods that create a task from lambda for you:


#### One-Time tasks
To schedule a task to be executed once at a given moment use `scheduleAt`
```kotlin
val momentStr = "5th July 2035, 16:52"
val momentODT = OffsetDateTime.parse(
    "$momentStr+00:00",
    DateTimeFormatter.ofPattern("d['th']['st']['nd']['rd'] MMMM yyyy, HH:mm")
)
scheduler.scheduleAt("Name1", momentODT) {
    println("This lambda will be executed on $momentStr")
}
```
To schedule a task that will execute once after a given interval of time use `scheduleAfter`:
```kotlin
scheduler.scheduleAfter("Name2", Duration.ofSeconds(8)) {
    println("You will see this message in 8 seconds after scheduleAfter")
}
```
All methods scheduling a lambda may also accept `RetryPolicy` as the third parameter:
```kotlin
scheduler.scheduleAfter(
    "Name3",
    Duration.ofSeconds(8),
    RetryPolicy.FixedDelay(3u, Duration.ofSeconds(2))
)
{
    println("This task will not fall, but if it did," +
            "it would be rescheduled to execute in 2s")
}
```


#### Recurring tasks
Task that repeats with a given period:
```kotlin
scheduler.scheduleRecurring("Name4", Duration.ofDays(1),
    RetryPolicy.ExpBackoff(3u, Duration.ofSeconds(2))) {
    println("Task Name4 is normally executed once a day")
    if (Random.nextInt(0, 3) == 0) {
        throw RuntimeException("Task Name4 failed")
    }
}
```
Task that is executed at moments described by a cron expression in Quartz format:
```kotlin
scheduler.scheduleRecurring("Name5", "0 15 10 ? * MON-FRI") {
      println("Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday")
  }
```

**Note:** If a reccuring task crashes several times in a row and exhausts a given retry attempts limit,
it will not execute at a designated moment next time. The only way to return the task back to work is to manually
reschedule it from a dashboard or via API.


#### Custom tasks
For more sophisticated purposes create an object of type Task and schedule it. In the example below we recreated a 
recurring task that repeats every 5s.
```kotlin
val myTaskName = TaskName("Name6")
val myTask = object: Task(
    myTaskName,
    RetryPolicy.FixedDelay(1u),
    successHandlers = listOf(
        { println("Will be printed first on successful completion") },
        { taskManager ->
            val moment = OffsetDateTime.now().plusSeconds(5)
            taskManager.schedule(myTaskName, moment)
        }
    )
)
{
    override fun run() {
        println("Your logic here")
    }

}

scheduler.scheduleAt(myTask, OffsetDateTime.now())
```


### RetryPolicy
There are two retry policies: with fixed delay and exponential backoff. Both of them accept a limit of retries
as the first parameter. 0 means that the task will not be retried, better use `retryPolicy = null` for that.
```kotlin
// If a task throws an exception, it will be repeated in 10s.
// If it falls again, it will not be further retried.
RetryPolicy.FixedDelay(1u, Duration.ofSeconds(10))
```
Exponential backoff accepts two parameters after retry limit. They are first delay and multiplier.
```kotlin
// If a task throws an exception, it will be repeated in 1s.
// If it falls again, it will be rescheduled to execute in 2.5s from the moment of this fall.
// If it falls the third time in a row, it will be rescheduled to execute in 1s * 2.5 * 2.5 = 6.25s.
// If it falls the fourth time, it will not be further retried.
RetryPolicy.ExpBackoff(3u, Duration.ofSeconds(1), 2.5)
```


### Chaining
Chain steps are also tasks and must also have **unique names** among all tasks.

Methods `andThen` and `orElse` allow to specify which step to execute next on success or failure of a current step. 
Success is a successful completion of one of the retries, while failure comes with the failure of the last retry attempt.

A step may have _several continuations_ for success and failure cases. In the example below if step one does not throw
an execution, both two and three will be scheduled.
```kotlin
val one = Chain("StepOne", RetryPolicy.FixedDelay(2u, Duration.ofSeconds(1))){
    if (Random.nextInt(0, 2) == 0) {
        println("StepOne failed")
        throw RuntimeException("Unexpected error")
    } else {
        println("StepOne completed")
    }
}
val two = Chain("StepTwo", null) { println("Step two") }
val three = Chain("StepThree", null) { println("Step three") }

one.andThen(two)
one.andThen(three)

scheduler.scheduleAfter(one, Duration.ZERO)
```
One can think of a whole chained system is a graph with _onSuccess_ and _onFailed_ edges, and of chain as a path in it.
To schedule a graph, schedule any chain that begins in a desired starting vertex.
```kotlin
val four = Chain("StepFour", null) { println("Step four") }
    
val oneTwo = one.andThen(two)
val threeFour = three.andThen(four)
val oneTwoThreeFour = oneTwo.orElse(threeFour)
// graph one --succ--> two --fail--> three --succ--> four
```
In the example above, to schedule the whole chain, schedule any of `one`, `oneTwo` or `oneTwoThreeFour`, but only one of them.
Note that if `one` fails, the chain will be interrupted and no other steps will be executed.

To make chains recurring, use `repeat`. This method just connects the end of a chain-path to the beginning of chain-path.

Let's say you want to run `one`. Then, if it is successful, continue with `two`.
If `one` fails, you want to run a backup-plan `three`. You also want to repeat this whole construction once 5 minutes.
Then you need to glue both ends to the beginning, therefore we specify repeat twice:
```kotlin
one.andThen(two).repeat(Duration.ofMinutes(5))
one.orElse(three).repeat(Duration.ofMinutes(5))
scheduler.scheduleAfter(one, Duration.ZERO)
```

**Note**: If a chain allows for several instances of the same task to be executed simultaneously,
the program is considered ill-formed.