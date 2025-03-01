package dev.su386.calina.tasks
import androidx.compose.runtime.mutableStateOf
import dev.su386.calina.Calina.exitAppSafely
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
object TaskManager {
    private val tasks = mutableListOf<ScheduledTask>()
    private var closing = false

    init {
        register(OnStartTask("Run Task Manager") {
            GlobalScope.launch {
                try {
                    run()
                } catch (e: Exception) {
                    println("Critical error: TaskManager has crashed. Exiting safely.")
                    e.printStackTrace()
                    exitAppSafely()
                }
            }
        })
    }

    fun register(task: ScheduledTask) {
        tasks.add(task)
    }

    private const val TICKS_PER_SECOND = 20
    private const val MILLISECONDS_PER_TICK = 1000 / TICKS_PER_SECOND
    private const val DELAY = (MILLISECONDS_PER_TICK * 0.3).toLong()

    suspend fun run() {
        println("Called once")
        var lastRunTime = 0L
        while (!closing) {
            if (System.currentTimeMillis() < lastRunTime + MILLISECONDS_PER_TICK) {
                delay(DELAY)
                continue
            }

            val tasksToRemove = mutableListOf<ScheduledTask>()
            for (task in tasks) {
                if (System.currentTimeMillis() < task.runAt) {
                    continue
                }
                tasksToRemove.add(task)

                task()
            }
            tasks.removeAll(tasksToRemove)

            lastRunTime = System.currentTimeMillis()
        }
    }

    fun onStart() {
        runBlocking {
            val jobs = mutableListOf<Deferred<Unit>>()

            val filteredTasks = tasks
                .filterIsInstance<OnStartTask>()

            tasks.removeAll(filteredTasks)
            val count = filteredTasks.size
            filteredTasks.forEach { jobs.add(it.runBlocking(this@runBlocking)) }

            jobs.awaitAll()
            println("Completed running $count start-up tasks")
        }
    }

    fun onClose() {
        closing = true
        runBlocking {
            val jobs = mutableListOf<Deferred<Unit>>()

            val filtedTasks = tasks
                .filterIsInstance<OnCloseTask>()

            filtedTasks.forEach { jobs.add(it.runBlocking(this@runBlocking)) }

            jobs.awaitAll()

            println("Completed running ${filtedTasks.size} close tasks")
        }
    }
}