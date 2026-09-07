package com.dansplugins.factionsystem.command.gate.remove

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.Block
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
class MfGateRemoveCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var gateService: MfGateService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var faction: MfFaction
    private lateinit var scheduler: BukkitScheduler
    private lateinit var uut: MfGateRemoveCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfGateRemoveCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.gate")).thenReturn(false)
        `when`(language["CommandGateRemoveNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_removesTheGateClosestToTheSender() {
        // prepare — two gates in the sender's world, the nearer one three blocks away
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderLocation(player, worldId, 0, 64, 0)
        val nearGate = gateAt(worldId, 3, 64, 0)
        val farGate = gateAt(worldId, 9, 64, 0)
        stubFactionGates(farGate, nearGate)
        stubDeletionOf(nearGate, farGate)
        `when`(language["CommandGateRemoveSuccess"]).thenReturn("Gate removed")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(gateService).delete(nearGate.id)
        verify(gateService, never()).delete(farGate.id)
        verify(player).sendMessage("${ChatColor.GREEN}Gate removed")
    }

    @Test
    fun testOnCommand_prefersAGateInTheSendersWorldOverAnIdenticallyPlacedGateElsewhere() {
        // prepare — the same coordinates hold a gate in each of two worlds, and the other world's gate is
        // measured first. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1993.
        val player = fixture.player
        val command = fixture.command
        val overworldId = UUID.randomUUID()
        val netherId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderLocation(player, overworldId, 100, 64, 100)
        val netherGate = gateAt(netherId, 100, 64, 100)
        val overworldGate = gateAt(overworldId, 100, 64, 100)
        stubFactionGates(netherGate, overworldGate)
        stubDeletionOf(overworldGate, netherGate)
        `when`(language["CommandGateRemoveSuccess"]).thenReturn("Gate removed")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify — the gate the sender is standing in is removed, not the one in the other world
        assertTrue(result)
        verify(gateService).delete(overworldGate.id)
        verify(gateService, never()).delete(netherGate.id)
        verify(player).sendMessage("${ChatColor.GREEN}Gate removed")
    }

    @Test
    fun testOnCommand_removesNothingWhenEveryGateIsInAnotherWorld() {
        // prepare — the sender stands at the gate's coordinates but in a different world
        val player = fixture.player
        val command = fixture.command
        val overworldId = UUID.randomUUID()
        val netherId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderLocation(player, overworldId, 100, 64, 100)
        val netherGate = gateAt(netherId, 100, 64, 100)
        stubFactionGates(netherGate)
        stubDeletionOf(netherGate)
        `when`(language["CommandGateRemoveFailedToFindGate"]).thenReturn("No gate found")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(gateService, never()).delete(netherGate.id)
        verify(player).sendMessage("${ChatColor.RED}No gate found")
    }

    @Test
    fun testOnCommand_removesNothingWhenTheOnlyGateIsTensOfThousandsOfBlocksAway() {
        // prepare — 60,000 blocks away squares to 3,600,000,000, which does not fit in an Int.
        // See https://github.com/Dans-Plugins/Medieval-Factions/issues/1991.
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        stubMembershipOf(player)
        stubSenderLocation(player, worldId, 0, 64, 0)
        val distantGate = gateAt(worldId, 60000, 64, 0)
        stubFactionGates(distantGate)
        stubDeletionOf(distantGate)
        `when`(language["CommandGateRemoveFailedToFindGate"]).thenReturn("No gate found")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify — the distance is well beyond gates.maxRemoveDistance, so nothing is removed
        assertTrue(result)
        verify(gateService, never()).delete(distantGate.id)
        verify(player).sendMessage("${ChatColor.RED}No gate found")
    }

    @Test
    fun testOnCommand_removesTheGateClosestToWhereTheSenderStoodWhenTheCommandWasRun() {
        // prepare — the sender walks 100 blocks away, well beyond gates.maxRemoveDistance, while the removal is
        // still queued. See https://github.com/Dans-Plugins/Medieval-Factions/issues/2006.
        val player = fixture.player
        val command = fixture.command
        val worldId = UUID.randomUUID()
        val pendingAsyncTasks = mutableListOf<Runnable>()
        // The dispatched task is queued rather than run, so the sender can be moved in between.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingAsyncTasks += invocation.arguments[1] as Runnable
            null
        }
        stubMembershipOf(player)
        stubSenderLocation(player, worldId, 0, 64, 0)
        val nearGate = gateAt(worldId, 3, 64, 0)
        stubFactionGates(nearGate)
        stubDeletionOf(nearGate)
        `when`(language["CommandGateRemoveSuccess"]).thenReturn("Gate removed")
        `when`(language["CommandGateRemoveFailedToFindGate"]).thenReturn("No gate found")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())
        stubSenderLocation(player, worldId, 100, 64, 100)
        pendingAsyncTasks.forEach(Runnable::run)

        // verify — the gate is measured from where the sender stood when the command was run
        assertTrue(result)
        verify(gateService).delete(nearGate.id)
        verify(player).sendMessage("${ChatColor.GREEN}Gate removed")
    }

    // Helper functions

    private fun gateAt(worldId: UUID, x: Int, y: Int, z: Int): MfGate {
        val position = MfBlockPosition(worldId, x, y, z)
        return MfGate(
            plugin,
            MfGateId.generate(),
            0,
            MfFactionId.generate(),
            MfCuboidArea(position, position),
            position,
            Material.IRON_BARS
        )
    }

    private fun stubFactionGates(vararg gates: MfGate) {
        `when`(gateService.getGatesByFaction(faction.id)).thenReturn(gates.toList())
    }

    /**
     * Stubs deletion of every gate a test knows about, including the ones it expects to be left alone. A gate the
     * command was never meant to touch is therefore reported by the `never()` verification rather than by an
     * exception on an unstubbed call, which keeps the failure legible if this protection ever regresses.
     */
    private fun stubDeletionOf(vararg gates: MfGate) {
        gates.forEach { gate -> `when`(gateService.delete(gate.id)).thenReturn(Success(Unit)) }
    }

    private fun stubMembershipOf(player: Player) {
        val mfPlayer = mock(MfPlayer::class.java)
        val role = mock(MfFactionRole::class.java)
        val removeGate = MfFactionPermission("REMOVE_GATE", "Remove gate", false)
        val factionPermissions = mock(MfFactionPermissions::class.java)
        `when`(player.hasPermission("mf.gate")).thenReturn(true)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(role)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        `when`(factionPermissions.removeGate).thenReturn(removeGate)
        `when`(role.hasPermission(faction, removeGate)).thenReturn(true)
    }

    private fun stubSenderLocation(player: Player, worldId: UUID, x: Int, y: Int, z: Int) {
        val world = testUtils.createMockWorld(worldId)
        val location = mock(Location::class.java)
        val block = mock(Block::class.java)
        `when`(location.world).thenReturn(world)
        `when`(world.getBlockAt(location)).thenReturn(block)
        `when`(block.world).thenReturn(world)
        `when`(block.x).thenReturn(x)
        `when`(block.y).thenReturn(y)
        `when`(block.z).thenReturn(z)
        `when`(player.location).thenReturn(location)
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        gateService = mock(MfGateService::class.java)
        `when`(services.gateService).thenReturn(gateService)

        faction = mock(MfFaction::class.java)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getInt("gates.maxRemoveDistance")).thenReturn(10)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // Run the dispatched task synchronously so the command body executes within the test.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
    }

    private fun mockLogger() {
        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }
}
