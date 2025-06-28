package dev.su386.calina.tasks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async

/**
 * Runs when application is closed
 *
 * @param taskName - Name of the task
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 */
class OnCloseTask(
    taskName: String,
    coroutineDispatcher: CoroutineDispatcher = Default,
    onRun: suspend () -> Unit
): ScheduledTask(
    taskName = taskName,
    runIn = Long.MAX_VALUE,
    coroutineDispatcher = coroutineDispatcher,
    onRun = onRun
) {
    override val runAt = Long.MAX_VALUE
    fun runBlocking(scope: CoroutineScope, also: (OnCloseTask) -> Unit = {}): Deferred<Unit> {
        println("Running task ${this.taskName}")
        return scope.async(coroutineDispatcher) {
            onRun()
            also(this@OnCloseTask)
        }

    }
}