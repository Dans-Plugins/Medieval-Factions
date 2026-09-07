package com.dansplugins.factionsystem.command.duel.accept

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.duel.MfDuelInvite
import com.dansplugins.factionsystem.duel.MfDuelService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfDuelAcceptCommandTest {
    private val testUtils = TestUtils()
    private val worldId = UUID.randomUUID()
    private val senderMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))
    private val targetMfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var server: Server
    private lateinit var playerService: MfPlayerService
    private lateinit var duelService: MfDuelService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var world: World
    private lateinit var pendingTasks: MutableList<Runnable>
    private var savedDuel: MfDuel? = null
    private lateinit var uut: MfDuelAcceptCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        pendingTasks = mutableListOf()
        savedDuel = null
        world = testUtils.createMockWorld(worldId)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfDuelAcceptCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandDuelAcceptNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_senderThatIsNotAPlayer() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.duel")).thenReturn(true)
        `when`(language["CommandDuelAcceptNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_senderWithoutATarget() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        `when`(player.hasPermission("mf.duel")).thenReturn(true)
        `when`(language["CommandDuelAcceptUsage"]).thenReturn("Usage")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.RED}Usage")
    }

    @Test
    fun testOnCommand_recordsTheStartingPositions() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        val target = stubTarget()
        stubAcceptableInvite(player, target)
        stubPosition(player, 10.0, 64.0, 10.0)
        stubPosition(target, 20.0, 64.0, 20.0)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Target"))
        runPendingTasks()

        // verify — the challenger is the inviter, which is the target here
        assertTrue(result)
        assertEquals(MfPosition(worldId, 20.0, 64.0, 20.0, 0.0f, 0.0f), savedDuel?.challengerLocation)
        assertEquals(MfPosition(worldId, 10.0, 64.0, 10.0, 0.0f, 0.0f), savedDuel?.challengedLocation)
    }

    @Test
    fun testOnCommand_recordsThePositionsHeldWhenTheCommandWasRun() {
        // prepare — both players keep moving while the duel is still queued. The duel is meant to record where they
        // stood when the challenge was accepted, since that is what returns them there afterwards.
        // See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        val target = stubTarget()
        stubAcceptableInvite(player, target)
        stubPosition(player, 10.0, 64.0, 10.0)
        stubPosition(target, 20.0, 64.0, 20.0)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf("Target"))
        stubPosition(player, 500.0, 64.0, 500.0)
        stubPosition(target, 600.0, 64.0, 600.0)
        runPendingTasks()

        // verify — the positions held when the command was run are the ones recorded, not the ones walked to
        assertTrue(result)
        assertEquals(MfPosition(worldId, 20.0, 64.0, 20.0, 0.0f, 0.0f), savedDuel?.challengerLocation)
        assertEquals(MfPosition(worldId, 10.0, 64.0, 10.0, 0.0f, 0.0f), savedDuel?.challengedLocation)
    }

    // Helper functions

    private fun stubTarget(): Player {
        val target = mock(Player::class.java)
        `when`(target.name).thenReturn("Target")
        `when`(server.getPlayer("Target")).thenReturn(target)
        return target
    }

    private fun stubAcceptableInvite(player: Player, target: Player) {
        val invite = MfDuelInvite(targetMfPlayer.id, senderMfPlayer.id)
        `when`(player.hasPermission("mf.duel")).thenReturn(true)
        `when`(player.name).thenReturn("Sender")
        `when`(playerService.getPlayer(player)).thenReturn(senderMfPlayer)
        `when`(playerService.getPlayer(target)).thenReturn(targetMfPlayer)
        `when`(duelService.getInvite(targetMfPlayer.id, senderMfPlayer.id)).thenReturn(invite)
        `when`(duelService.deleteInvite(targetMfPlayer.id, senderMfPlayer.id)).thenReturn(Success(Unit))
        `when`(duelService.save(anyDuel<MfDuel>())).thenAnswer { invocation ->
            val duel = invocation.arguments[0] as MfDuel
            savedDuel = duel
            Success(duel)
        }
        `when`(language["CommandDuelAcceptSuccess", "Target"]).thenReturn("Duel accepted")
        `when`(language["CommandDuelAcceptChallengeAccepted", "Sender"]).thenReturn("Challenge accepted")
    }

    private fun stubPosition(player: Player, x: Double, y: Double, z: Double) {
        val location = mock(Location::class.java)
        `when`(location.world).thenReturn(world)
        `when`(location.x).thenReturn(x)
        `when`(location.y).thenReturn(y)
        `when`(location.z).thenReturn(z)
        `when`(player.location).thenReturn(location)
    }

    /**
     * Mockito's [ArgumentMatchers.any] returns null, which trips Kotlin's null-check on the non-nullable [MfDuel]
     * parameter of [MfDuelService.save] before the matcher is registered, corrupting Mockito's matcher stack for
     * subsequent tests. This generic indirection avoids the compiler inserting that check. The type argument is
     * given explicitly at the call site, since [MfDuelService.save] is overloaded.
     */
    private fun <T> anyDuel(): T {
        ArgumentMatchers.any<MfDuel>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun runPendingTasks() {
        while (pendingTasks.isNotEmpty()) {
            pendingTasks.removeAt(0).run()
        }
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        duelService = mock(MfDuelService::class.java)
        `when`(services.duelService).thenReturn(duelService)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getString("duels.duration")).thenReturn("PT5M")
    }

    private fun mockScheduler() {
        server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // The dispatched task is queued rather than run, so a test can move the players in between the command being
        // run and the task executing — which is the case this command has to get right. The main-thread task the
        // command hops back to afterwards is deliberately left unqueued: it restores health, creates a boss bar and
        // notifies nearby players, none of which bears on which positions the duel recorded.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingTasks += invocation.arguments[1] as Runnable
            null
        }
    }

    private fun mockLogger() {
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }
}
