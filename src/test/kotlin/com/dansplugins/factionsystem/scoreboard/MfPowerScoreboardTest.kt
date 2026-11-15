package com.dansplugins.factionsystem.scoreboard

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.scoreboard.MfPowerScoreboard.Line
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Score
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class MfPowerScoreboardTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var manager: ScoreboardManager
    private lateinit var mainScoreboard: Scoreboard
    private lateinit var newScoreboard: Scoreboard
    private lateinit var objective: Objective
    private lateinit var score: Score
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val server = mock(Server::class.java)
        manager = mock(ScoreboardManager::class.java)
        mainScoreboard = mock(Scoreboard::class.java)
        newScoreboard = mock(Scoreboard::class.java)
        objective = mock(Objective::class.java)
        score = mock(Score::class.java)
        player = mock(Player::class.java)
        val config = mock(FileConfiguration::class.java)
        val language = mock(Language::class.java)
        val services = mock(Services::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val factionService = mock(MfFactionService::class.java)

        `when`(plugin.server).thenReturn(server)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.language).thenReturn(language)
        `when`(plugin.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(language["PowerScoreboardTitle"]).thenReturn("Power")
        `when`(language["PowerScoreboardPlayerPower"]).thenReturn("Your power")
        `when`(language["PowerScoreboardFactionPower"]).thenReturn("Faction power")
        `when`(language["PowerScoreboardFactionMaxPower"]).thenReturn("Faction max power")
        `when`(config.getDouble("players.initialPower")).thenReturn(5.0)
        `when`(server.scoreboardManager).thenReturn(manager)
        `when`(manager.mainScoreboard).thenReturn(mainScoreboard)
        `when`(manager.newScoreboard).thenReturn(newScoreboard)
        `when`(newScoreboard.registerNewObjective(MfPowerScoreboard.OBJECTIVE_NAME, "dummy", "Power")).thenReturn(objective)
        `when`(newScoreboard.entries).thenReturn(emptySet())
        `when`(objective.getScore("Your power")).thenReturn(score)
        `when`(player.uniqueId).thenReturn(UUID.randomUUID())
    }

    @Test
    fun `factionless player only sees their own power`() {
        val lines = MfPowerScoreboard.buildLines(7.9, null, null, "You", "Faction", "Max")

        assertEquals(listOf(Line("You", 7)), lines)
    }

    @Test
    fun `faction member sees player power, faction power and faction max power`() {
        val lines = MfPowerScoreboard.buildLines(7.9, 31.2, 40.0, "You", "Faction", "Max")

        assertEquals(listOf(Line("You", 7), Line("Faction", 31), Line("Max", 40)), lines)
    }

    @Test
    fun `player on the main scoreboard gets the power sidebar`() {
        `when`(player.scoreboard).thenReturn(mainScoreboard)

        MfPowerScoreboard(plugin).update(player)

        verify(player).scoreboard = newScoreboard
        verify(objective).displaySlot = DisplaySlot.SIDEBAR
        verify(score).score = 5
    }

    @Test
    fun `scoreboard set by another plugin is left alone`() {
        val otherPluginsScoreboard = mock(Scoreboard::class.java)
        `when`(player.scoreboard).thenReturn(otherPluginsScoreboard)

        MfPowerScoreboard(plugin).update(player)

        verify(manager, never()).newScoreboard
        verify(player, never()).scoreboard = newScoreboard
    }
}
