package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

/**
 * A class responsible for scheduling and managing tasks related to faction updates.
 *
 * @param plugin The MedievalFactions plugin instance.
 */
class TaskScheduler(private val plugin: MedievalFactions) {

    private val updateTasks: MutableMap<MfFactionId, MutableList<BukkitTask>> =
        Collections.synchronizedMap(mutableMapOf<MfFactionId, MutableList<BukkitTask>>())

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param factionId The ID of the faction for which the task is scheduled.
     * @param runnable The task to be executed.
     * @param delay The delay in ticks before the task is executed. Default is 0.
     */
    fun scheduleTask(factionId: MfFactionId, runnable: Runnable, delay: Long = 0L) {
        val updateTask = object : BukkitRunnable() {
            override fun run() {
                runnable.run()
                val factionUpdateTasks = updateTasks[factionId]
                factionUpdateTasks?.removeAll(plugin.server.scheduler.pendingTasks.filter { it.taskId == taskId })
            }
        }.runTaskLater(plugin, delay)
        val factionUpdateTasks = updateTasks[factionId]
        if (factionUpdateTasks == null) {
            updateTasks[factionId] = Collections.synchronizedList(mutableListOf(updateTask))
        } else {
            factionUpdateTasks.add(updateTask)
        }
    }

    /**
     * Cancels all scheduled tasks for a specified faction.
     *
     * @param factionId The ID of the faction whose tasks are to be cancelled.
     */
    fun cancelTasks(factionId: MfFactionId) {
        val factionUpdateTasks = updateTasks[factionId]
        if (!factionUpdateTasks.isNullOrEmpty()) {
            factionUpdateTasks.forEach(BukkitTask::cancel)
            factionUpdateTasks.clear()
        }
    }
}
