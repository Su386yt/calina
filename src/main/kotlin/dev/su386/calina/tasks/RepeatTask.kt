package dev.su386.calina.tasks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import java.util.*

/**
 * Repeated tasks to be executed after a specific cooldown
 *
 * @param taskName - Name of the task
 * @param taskCooldown - Time in ms between the executing of each task
 * @param persistentCooldown - Whether the cooldown time is saved on restarts. If true, and an app is restarted, the time since last active will save
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 *
 * @see dev.su386.calina.tasks.ScheduledTask
 */
class RepeatTask(
    taskName: String = UUID.randomUUID().toString(),
    val taskCooldown: Long,
    persistentCooldown: Boolean = false,
    coroutineDispatcher: CoroutineDispatcher = Default,
    onRun: () -> Unit
): ScheduledTask(
    taskName = taskName,
    runIn = taskCooldown,
    persistentTimer = persistentCooldown,
    coroutineDispatcher = coroutineDispatcher,
    onRun = {
        TaskManager.register(RepeatTask(
            taskName = taskName,
            taskCooldown = taskCooldown,
            persistentCooldown = persistentCooldown,
            onRun = onRun
        ))
        onRun()
    }
) {
    override val runIn get() = taskCooldown - System.currentTimeMillis() + createTime


}