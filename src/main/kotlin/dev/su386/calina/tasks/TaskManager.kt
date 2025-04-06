package dev.su386.calina.tasks
import androidx.compose.runtime.mutableStateOf
import dev.su386.calina.Calina.exitAppSafely
import dev.su386.calina.data.Database
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
object TaskManager {
    private val tasks = mutableListOf<ScheduledTask>()
    private var closing = false
    private var starting = true

    init {
        register(OnStartTask("Load Persistent Tasks") {
            loadPersistentTasks()
        })

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
     * Registers a [ScheduledTask] to be executed in the future. Only tasks with unique task names can be registered. This includes tasks that have been saved persistantly
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

    private fun loadPersistentTasks() {
        val persistentTasks = Database.readData<Array<ScheduledTask>>("tasks/persistent.json") ?: arrayOf()
        persistentTasks.forEach { register(it) }
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
            starting = false
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