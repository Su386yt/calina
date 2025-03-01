package dev.su386.calina.tasks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Repeated tasks to be executed after a specific cooldown
 *
 * @param taskName - Name of the task
 * @param runIn - Time in ms for when to execute the task
 * @param persistentTimer - Whether the scheduled task is saved on restarts. If true, and an app is restarted, the time since last active will save
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 */
open class ScheduledTask(
    open val taskName: String = UUID.randomUUID().toString(),
    open val runIn: Long,
    open val persistentTimer: Boolean = false,
    open val coroutineDispatcher: CoroutineDispatcher = Default,
    open val onRun: () -> Unit
) {
    open val createTime = System.currentTimeMillis()
    open val runAt get() = createTime + this.runIn

    @OptIn(DelicateCoroutinesApi::class)
    open operator fun invoke() {
        try {
            println("Running task ${this.taskName}")
            GlobalScope.launch(this.coroutineDispatcher){
                try {
                    onRun()
                } catch (e: Exception) {
                    println("Error running ${this@ScheduledTask.javaClass.simpleName} ${this@ScheduledTask.taskName}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            println("Error running ${this@ScheduledTask.javaClass.simpleName} ${this@ScheduledTask.taskName}")
            e.printStackTrace()
        }
    }
}