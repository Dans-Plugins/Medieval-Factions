package com.dansplugins.factionsystem.scoreboard

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID
import kotlin.math.floor

/**
 * Optional sidebar showing the player's power and their faction's power (`powerScoreboard.enabled`).
 *
 * Off by default. When on, each online player gets their own scoreboard, refreshed every
 * `powerScoreboard.updateIntervalSeconds`. A player whose scoreboard has been set by another plugin is left
 * alone, so this never replaces a scoreboard it did not create. Must be used from the server thread.
 */
class MfPowerScoreboard(private val plugin: MedievalFactions) {

    data class Line(val label: String, val value: Int)

    private val boards = mutableMapOf<UUID, Scoreboard>()

    fun start() {
        if (!plugin.config.getBoolean("powerScoreboard.enabled")) return
        val intervalTicks = plugin.config.getLong("powerScoreboard.updateIntervalSeconds").coerceAtLeast(1L) * 20L
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { updateAll() }, 20L, intervalTicks)
    }

    /** Gives every player still showing this sidebar the server's main scoreboard back. */
    fun stop() {
        val manager = plugin.server.scoreboardManager ?: return
        plugin.server.onlinePlayers
            .filter { player -> boards[player.uniqueId]?.let { it == player.scoreboard } == true }
            .forEach { it.scoreboard = manager.mainScoreboard }
        boards.clear()
    }

    fun updateAll() {
        val online = plugin.server.onlinePlayers
        val onlineIds = online.map { it.uniqueId }.toSet()
        boards.keys.retainAll(onlineIds)
        online.forEach { update(it) }
    }

    fun update(player: Player) {
        val manager = plugin.server.scoreboardManager ?: return
        val current = player.scoreboard
        var board = boards[player.uniqueId]
        if (current != manager.mainScoreboard && current != board) return
        if (board == null || current != board) {
            board = manager.newScoreboard
            boards[player.uniqueId] = board
            player.scoreboard = board
        }
        val objective = board.getObjective(OBJECTIVE_NAME)
            ?: board.registerNewObjective(OBJECTIVE_NAME, "dummy", plugin.language["PowerScoreboardTitle"])
        objective.displaySlot = DisplaySlot.SIDEBAR
        val lines = lines(player)
        board.entries.filter { entry -> lines.none { it.label == entry } }.forEach(board::resetScores)
        lines.forEach { objective.getScore(it.label).score = it.value }
    }

    fun lines(player: Player): List<Line> {
        val mfPlayer = plugin.services.playerService.getPlayer(player)
        val playerPower = mfPlayer?.power ?: plugin.config.getDouble("players.initialPower")
        val faction = mfPlayer?.let { plugin.services.factionService.getFaction(it.id) }
        return buildLines(
            playerPower = playerPower,
            factionPower = faction?.power,
            factionMaxPower = faction?.maxPower,
            playerPowerLabel = plugin.language["PowerScoreboardPlayerPower"],
            factionPowerLabel = plugin.language["PowerScoreboardFactionPower"],
            factionMaxPowerLabel = plugin.language["PowerScoreboardFactionMaxPower"]
        )
    }

    companion object {
        const val OBJECTIVE_NAME = "mf_power"

        fun buildLines(
            playerPower: Double,
            factionPower: Double?,
            factionMaxPower: Double?,
            playerPowerLabel: String,
            factionPowerLabel: String,
            factionMaxPowerLabel: String
        ): List<Line> = listOfNotNull(
            Line(playerPowerLabel, floor(playerPower).toInt()),
            factionPower?.let { Line(factionPowerLabel, floor(it).toInt()) },
            factionMaxPower?.let { Line(factionMaxPowerLabel, floor(it).toInt()) }
        )
    }
}
