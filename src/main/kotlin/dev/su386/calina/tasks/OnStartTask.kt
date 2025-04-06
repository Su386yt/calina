package dev.su386.calina.tasks

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import java.util.*

/**
 * Runs when application is started. Preferred over directly calling in main or init methods, as this allows for coroutine management, and allows for more precise logging and startup management.
 *
 * @param taskName - Name of the task
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 */
class OnStartTask(
    taskName: String,
    coroutineDispatcher: CoroutineDispatcher = Default,
    onRun: () -> Unit
): ScheduledTask(
    taskName = taskName,
    runIn = 0L, // If these get caught in the task queue and aren't called on start, they should get called instantly
    coroutineDispatcher = coroutineDispatcher,
    onRun = onRun
) {
    override val runAt = 0L // If these get caught in the task queue and aren't called on start, they should get called instantly

    fun runBlocking(scope: CoroutineScope, also: (OnStartTask) -> Unit = {}): Deferred<Unit> {
        println("Running task ${this.taskName}")
        return scope.async(coroutineDispatcher) {
            onRun()
            also(this@OnStartTask)
        }
    }
}