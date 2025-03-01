package dev.su386.calina.tasks

import dev.su386.calina.tasks.RepeatTask.TaskCooldown.Companion.ms
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
 * @param startImmediately - Whether the scheduled task should run the first time immediately or wait the cooldown
 * @param coroutineDispatcher - Dispatcher context to run the [onRun] function in
 * @param onRun - What to run when the task is called
 *
 * @see dev.su386.calina.tasks.ScheduledTask
 */
class RepeatTask(
    taskName: String = UUID.randomUUID().toString(),
    val taskCooldown: TaskCooldown,
    persistentCooldown: Boolean = false,
    startImmediately: Boolean = true,
    coroutineDispatcher: CoroutineDispatcher = Default,
    onRun: () -> Unit
): ScheduledTask(
    taskName = taskName,
    runIn = taskCooldown.duration,
    persistentTimer = persistentCooldown,
    coroutineDispatcher = coroutineDispatcher,
    onRun = {
        TaskManager.register(RepeatTask(
            taskName = taskName,
            taskCooldown = taskCooldown,
            startImmediately = false,
            persistentCooldown = persistentCooldown,
            onRun = onRun
        ))
        onRun()
    }
) {
    constructor(
        taskName: String = UUID.randomUUID().toString(),
        taskCooldown: Long,
        persistentCooldown: Boolean = false,
        startImmediately: Boolean = true,
        coroutineDispatcher: CoroutineDispatcher = Default,
        onRun: () -> Unit
    ) : this(
        taskName,
        taskCooldown.ms,
        persistentCooldown,
        startImmediately,
        coroutineDispatcher,
        onRun
    )

    override val createTime = if (startImmediately) {
        0
    } else {
        System.currentTimeMillis()
    }
    override val runIn get() = taskCooldown.duration - System.currentTimeMillis() + createTime


    /**
     * Allows for cooldowns to be passed by reference and be dynamic vs by value
     *
     * @param duration - Length of cooldown
     */
    class TaskCooldown(var duration: Long) {
        companion object {
            val Long.ms: TaskCooldown get() = TaskCooldown(this)
            val Int.ms: TaskCooldown get() = TaskCooldown(this.toLong())
        }
    }
}