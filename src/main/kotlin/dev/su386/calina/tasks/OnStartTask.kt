package dev.su386.calina.tasks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async

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
    onRun: suspend () -> Unit
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