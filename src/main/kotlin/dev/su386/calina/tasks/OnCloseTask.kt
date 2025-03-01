package dev.su386.calina.tasks

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import java.util.*

/**
 * Runs when application is closed
 *
 * @param taskName - Name of the task
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 */
class OnCloseTask(
    taskName: String = UUID.randomUUID().toString(),
    coroutineDispatcher: CoroutineDispatcher = Default,
    onRun: () -> Unit
): ScheduledTask(
    taskName = taskName,
    runIn = Long.MAX_VALUE,
    persistentTimer = false,
    coroutineDispatcher = coroutineDispatcher,
    onRun = onRun
) {
    override val runAt = Long.MAX_VALUE
    fun runBlocking(scope: CoroutineScope): Deferred<Unit> {
        println("Running task ${this.taskName}")
        return scope.async(coroutineDispatcher) {
            onRun()
        }

    }
}