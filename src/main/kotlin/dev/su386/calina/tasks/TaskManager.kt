package dev.su386.calina.tasks
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.su386.calina.Calina.exitAppSafely
import dev.su386.calina.app.App
import dev.su386.calina.app.App.navigationStack
import dev.su386.calina.utils.AutoResizeText
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DelicateCoroutinesApi::class)
object TaskManager {
    private val tasks = mutableListOf<ScheduledTask>()
    private var closing = false
    private var starting = true

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

    /**
     * Registers a [ScheduledTask] to be executed in the future. Only tasks with unique task names can be registered.
     *
     * @param task - Task to be executed
     * @return Returns false if the task with [task.taskName] has already been registered. Otherwise, returns true
     * @see dev.su386.calina.tasks.ScheduledTask
     */
    fun register(task: ScheduledTask): Boolean {
        if (tasks.count { it.taskName == task.taskName } != 0) {
            return false
        }

        tasks.add(task)
        return true
    }

    private const val TICKS_PER_SECOND = 20
    private const val MILLISECONDS_PER_TICK = 1000 / TICKS_PER_SECOND
    private const val DELAY = (MILLISECONDS_PER_TICK * 0.3).toLong()

    private suspend fun run() {
        println("Called once")
        var lastRunTime = 0L
        while (!closing) {
            if (System.currentTimeMillis() < lastRunTime + MILLISECONDS_PER_TICK || starting) {
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
        val tasksCompleted by mutableStateOf(AtomicInteger(0))

        val jobs = mutableListOf<Deferred<Unit>>()

        val filteredTasks = tasks
            .filterIsInstance<OnStartTask>()

        tasks.removeAll(filteredTasks)
        val count = filteredTasks.size

        navigationStack.add {
            Column(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AutoResizeText(
                    "Loading ${tasksCompleted.get()}/$count tasks",
                    modifier = Modifier.height(50.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier.height(25.dp)
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(80.dp)
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.background,
                )
            }
        }

        GlobalScope.launch {
            filteredTasks.forEach { jobs.add(it.runBlocking(this) { tasksCompleted.incrementAndGet() } ) }
            jobs.awaitAll()
            starting = false
            println("Completed running $count start-up tasks")
            App.closeAllPopups()
        }
    }

    fun onClose() {
        closing = true
        runBlocking {
            val jobs = mutableListOf<Deferred<Unit>>()

            val filteredTasks = tasks
                .filterIsInstance<OnCloseTask>()

            filteredTasks.forEach { jobs.add(it.runBlocking(this@runBlocking)) }

            jobs.awaitAll()

            println("Completed running ${filteredTasks.size} close tasks")
        }
    }
}