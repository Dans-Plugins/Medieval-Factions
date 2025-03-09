package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class TaskScheduler(private val plugin: MedievalFactions) {

    private val updateTasks: MutableMap<MfFactionId, MutableList<BukkitTask>> =
        Collections.synchronizedMap(mutableMapOf<MfFactionId, MutableList<BukkitTask>>())

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

    fun cancelTasks(factionId: MfFactionId) {
        val factionUpdateTasks = updateTasks[factionId]
        if (!factionUpdateTasks.isNullOrEmpty()) {
            factionUpdateTasks.forEach(BukkitTask::cancel)
            factionUpdateTasks.clear()
        }
    }
}
