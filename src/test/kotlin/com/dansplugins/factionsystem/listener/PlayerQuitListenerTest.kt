package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import com.dansplugins.factionsystem.teleport.MfTeleportService
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Level.SEVERE
import java.util.logging.Logger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerQuitListenerTest {

    private lateinit var fixture: PlayerQuitListenerTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var interactionService: MfInteractionService
    private lateinit var entityInteractionProtection: EntityInteractionProtection
    private lateinit var teleportService: MfTeleportService
    private lateinit var logger: Logger
    private lateinit var config: FileConfiguration
    private lateinit var scheduler: BukkitScheduler
    private val pendingAsyncTasks = mutableListOf<Runnable>()
    private lateinit var uut: PlayerQuitListener

    @BeforeEach
    fun setUp() {
        pendingAsyncTasks.clear()
        fixture = createFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockConfig()
        mockLogger()
        mockScheduler()
        `when`(playerService.save(anyMfPlayer())).thenReturn(Success(fixture.mfPlayer))
        entityInteractionProtection = mock(EntityInteractionProtection::class.java)
        uut = PlayerQuitListener(plugin, entityInteractionProtection)
    }

    @Test
    fun onPlayerQuit_ShouldNotSaveThePlayerOnTheServerThread() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)

        // Assert - the save must not have happened while the server thread was inside the listener
        verify(playerService, never()).save(anyMfPlayer())
        verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable::class.java))

        runPendingAsyncTasks()

        verify(playerService).save(fixture.mfPlayer.copy(powerAtLogout = fixture.mfPlayer.power))
    }

    @Test
    fun onPlayerQuit_ShouldSaveThePowerHeldAtQuitTimeAsPowerAtLogout() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)
        runPendingAsyncTasks()

        // Assert
        val saved = capturedSavedPlayer()
        assertEquals(12.5, saved.powerAtLogout)
        assertEquals(12.5, saved.power)
        assertEquals(fixture.playerId, saved.id)
    }

    @Test
    fun onPlayerQuit_CachedPlayerChangesBeforeTheSaveRuns_ShouldSaveTheSnapshotTakenOnTheServerThread() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)
        // Something else mutates the cached player between dispatch and the save actually running,
        // as a reconnect or a power cycle would.
        `when`(playerService.getPlayer(fixture.player))
            .thenReturn(fixture.mfPlayer.copy(power = 99.0, powerAtLogout = 99.0))
        runPendingAsyncTasks()

        // Assert - the value snapshotted at quit time is what gets written
        val saved = capturedSavedPlayer()
        assertEquals(12.5, saved.powerAtLogout)
        assertEquals(12.5, saved.power)
    }

    @Test
    fun onPlayerQuit_ShouldUnloadInteractionStatusOnTheServerThread() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)

        // Assert - unloading must be done before the listener returns, so that a reconnect's
        // loadInteractionStatus cannot be undone by a late unload
        verify(interactionService).unloadInteractionStatus(fixture.playerId)
    }

    @Test
    fun onPlayerQuit_SaveFails_ShouldLogSevereAndStillHaveUnloadedInteractionStatus() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)
        val cause = RuntimeException("database is down")
        `when`(playerService.save(anyMfPlayer()))
            .thenReturn(Failure(ServiceFailure(GENERAL, "Service error: database is down", cause)))

        // Act
        uut.onPlayerQuit(fixture.event)
        runPendingAsyncTasks()

        // Assert
        verify(logger).log(SEVERE, "Failed to save player: Service error: database is down", cause)
        verify(interactionService).unloadInteractionStatus(fixture.playerId)
    }

    @Test
    fun onPlayerQuit_PlayerIsNotInDatabase_ShouldSaveANewPlayerWithInitialPower() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(null)
        `when`(config.getDouble("players.initialPower")).thenReturn(20.0)

        // Act
        uut.onPlayerQuit(fixture.event)

        // Assert
        verify(playerService, never()).save(anyMfPlayer())

        runPendingAsyncTasks()

        val saved = capturedSavedPlayer()
        assertEquals(fixture.playerId, saved.id)
        assertEquals(20.0, saved.power)
        assertEquals(20.0, saved.powerAtLogout)
    }

    @Test
    fun onPlayerQuit_ShouldDropRetainedEntityNotificationStateOnTheServerThread() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)

        // Assert - the retained notification is keyed on the player's UUID, so it has to be dropped
        // before the listener returns, or a reconnecting player inherits the de-duplication window
        // that was open when they left
        verify(entityInteractionProtection).forgetPlayer(fixture.player.uniqueId)
    }

    @Test
    fun onPlayerQuit_ShouldCancelTeleportationOnTheServerThread() {
        // Arrange
        `when`(playerService.getPlayer(fixture.player)).thenReturn(fixture.mfPlayer)

        // Act
        uut.onPlayerQuit(fixture.event)

        // Assert
        verify(teleportService).cancelTeleportation(fixture.player)
    }

    // Helper functions

    private fun createFixture(): PlayerQuitListenerTestFixture {
        val uuid = UUID.randomUUID()
        val player = mock(Player::class.java)
        `when`(player.uniqueId).thenReturn(uuid)
        `when`(player.name).thenReturn("testPlayer")
        val event = mock(PlayerQuitEvent::class.java)
        `when`(event.player).thenReturn(player)
        val playerId = MfPlayerId(uuid.toString())
        val mfPlayer = MfPlayer(playerId, version = 3, name = "testPlayer", power = 12.5, powerAtLogout = 4.0)
        return PlayerQuitListenerTestFixture(player, event, playerId, mfPlayer)
    }

    private data class PlayerQuitListenerTestFixture(
        val player: Player,
        val event: PlayerQuitEvent,
        val playerId: MfPlayerId,
        val mfPlayer: MfPlayer
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        interactionService = mock(MfInteractionService::class.java)
        `when`(services.interactionService).thenReturn(interactionService)

        teleportService = mock(MfTeleportService::class.java)
        `when`(services.teleportService).thenReturn(teleportService)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    private fun mockLogger() {
        logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        // The dispatched task is queued rather than run, so that what the listener did on the server
        // thread can be told apart from what it deferred.
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            pendingAsyncTasks += invocation.arguments[1] as Runnable
            null
        }
    }

    private fun runPendingAsyncTasks() {
        val tasks = pendingAsyncTasks.toList()
        pendingAsyncTasks.clear()
        tasks.forEach { it.run() }
    }

    private fun capturedSavedPlayer(): MfPlayer {
        val captor = org.mockito.ArgumentCaptor.forClass(MfPlayer::class.java)
        verify(playerService).save(captor.capture() ?: fixture.mfPlayer)
        return captor.value
    }

    private fun <T> anyMfPlayer(): T {
        ArgumentMatchers.any<MfPlayer>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
