package com.dansplugins.factionsystem.command.faction.map

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionMapCommandTest {
    private val testUtils = TestUtils()
    private val mfPlayer = MfPlayer(MfPlayerId(UUID.randomUUID().toString()))

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var overworld: World
    private lateinit var nether: World
    private lateinit var pendingTasks: MutableList<Runnable>
    private lateinit var uut: MfFactionMapCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        pendingTasks = mutableListOf()
        overworld = testUtils.createMockWorld()
        nether = testUtils.createMockWorld()
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfFactionMapCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(language["CommandFactionMapNoPermission"]).thenReturn("No permission")

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
        `when`(sender.hasPermission("mf.map")).thenReturn(true)
        `when`(language["CommandFactionMapNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_rendersTheWorldTheSenderIsStandingIn() {
        // prepare
        val player = fixture.player
        val command = fixture.command
        stubFactionlessPlayer(player)
        stubSenderChunk(player, overworld, 0, 0)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        runPendingTasks()

        // verify — the grid is centred on the sender's chunk and 21x9 cells wide
        assertTrue(result)
        verify(claimService).getClaim(overworld, 0, 0)
        verify(claimService).getClaim(overworld, -10, -4)
        verify(claimService).getClaim(overworld, 10, 4)
    }

    @Test
    fun testOnCommand_rendersTheWorldTheSenderStoodInWhenTheCommandWasRun() {
        // prepare — the sender changes world while the map render is still queued. The chunk coordinates are already
        // snapshotted on the main thread, so a world read taken at task time would index the arrived-in world by
        // coordinates taken from the departed one. See https://github.com/Dans-Plugins/Medieval-Factions/issues/2020.
        val player = fixture.player
        val command = fixture.command
        stubFactionlessPlayer(player)
        stubSenderChunk(player, overworld, 0, 0)

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderChunk(player, nether, 64, 64)
        runPendingTasks()

        // verify — the map is of the world the sender stood in, not the one they travelled to
        assertTrue(result)
        verify(claimService).getClaim(overworld, 0, 0)
        verify(claimService, never()).getClaim(nether, 0, 0)
        verify(claimService, never()).getClaim(nether, 64, 64)
    }

    // Helper functions

    private fun stubFactionlessPlayer(player: Player) {
        `when`(player.hasPermission("mf.map")).thenReturn(true)
        `when`(player.spigot()).thenReturn(mock(Player.Spigot::class.java))
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(null)
    }

    private fun stubSenderChunk(player: Player, world: World, chunkX: Int, chunkZ: Int) {
        val chunk = testUtils.createMockChunk(world, chunkX, chunkZ)
        val location = mock(Location::class.java)
        `when`(location.chunk).thenReturn(chunk)
        `when`(location.world).thenReturn(world)
        `when`(player.location).thenReturn(location)
        `when`(player.world).thenReturn(world)
    }

    private fun runPendingTasks() {
        while (pendingTasks.isNotEmpty()) {
            pendingTasks.removeAt(0).run()
        }
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        `when`(services.factionRelationshipService).thenReturn(mock(MfFactionRelationshipService::class.java))
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
        `when`(language["Wilderness"]).thenReturn("Wilderness")
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getString("wilderness.color")).thenReturn("#FFFFFF")
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // Every task the command schedules is queued rather than run, so a test can move the sender between the
        // command being run and the task executing — which is the case this command has to get right.
        // `runPendingTasks` then drains the queue in the order the tasks were dispatched.
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
