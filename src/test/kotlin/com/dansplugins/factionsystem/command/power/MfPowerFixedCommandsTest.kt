package com.dansplugins.factionsystem.command.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.power.clearfixed.MfPowerClearFixedCommand
import com.dansplugins.factionsystem.command.power.set.MfPowerSetCommand
import com.dansplugins.factionsystem.command.power.setfixed.MfPowerSetFixedCommand
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger

/** `/power setfixed` and `/power clearfixed` (#1682), and how `/power set` treats a fixed player. */
class MfPowerFixedCommandsTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var playerService: MfPlayerService
    private lateinit var sender: CommandSender
    private lateinit var target: OfflinePlayer
    private val command = mock(Command::class.java)
    private val targetId = MfPlayerId(UUID.randomUUID().toString())

    /** The player each `update` was last given the result of, after its transform ran. */
    private var saved: MfPlayer? = null

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        // Messages are their key followed by their parameters.
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == Locale::class.java) {
                Locale.US
            } else {
                invocation.arguments.flatMap { (it as? Array<*>)?.toList() ?: listOf(it) }.joinToString(" ")
            }
        }
        `when`(plugin.language).thenReturn(language)
        val config = mock(FileConfiguration::class.java)
        `when`(config.getDouble("players.minPower")).thenReturn(0.0)
        `when`(config.getDouble("players.maxPower")).thenReturn(20.0)
        `when`(config.getDouble("players.initialPower")).thenReturn(5.0)
        `when`(plugin.config).thenReturn(config)

        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable::class.java))).thenAnswer { invocation ->
            (invocation.arguments[1] as Runnable).run()
            null
        }
        target = mock(OfflinePlayer::class.java)
        `when`(target.name).thenReturn("Alfred")
        `when`(target.uniqueId).thenReturn(UUID.fromString(targetId.value))
        `when`(target.hasPlayedBefore()).thenReturn(true)
        `when`(server.getOfflinePlayer("Alfred")).thenReturn(target)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        sender = mock(CommandSender::class.java)
        `when`(sender.hasPermission("mf.power.fixed")).thenReturn(true)
    }

    private fun givenTarget(player: MfPlayer) {
        `when`(playerService.getPlayer(target)).thenReturn(player)
        `when`(playerService.update(eqPlayer(player), anyTransform())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val transform = invocation.arguments[1] as (MfPlayer) -> MfPlayer
            Success(transform(player).also { saved = it })
        }
    }

    @Test
    fun `setfixed needs mf_power_fixed or mf_force_power`() {
        `when`(sender.hasPermission("mf.power.fixed")).thenReturn(false)

        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "10"))

        verify(sender).sendMessage("${RED}CommandPowerFixedNoPermission")
        verify(playerService, never()).update(anyPlayer(), anyTransform())
    }

    @Test
    fun `setfixed with mf_force_power is allowed`() {
        `when`(sender.hasPermission("mf.power.fixed")).thenReturn(false)
        `when`(sender.hasPermission("mf.force.power")).thenReturn(true)
        givenTarget(MfPlayer(targetId, power = 3.0, powerAtLogout = 3.0))

        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "10"))

        assertEquals(10.0, saved?.fixedPower)
    }

    @Test
    fun `setfixed pins power, power at logout and the fixed amount to the value`() {
        givenTarget(MfPlayer(targetId, power = 3.0, powerAtLogout = 2.0))

        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "12.5"))

        assertEquals(12.5, saved?.power)
        assertEquals(12.5, saved?.powerAtLogout)
        assertEquals(12.5, saved?.fixedPower)
        verify(sender).sendMessage("${GREEN}CommandPowerSetFixedSuccess Alfred 12.5")
    }

    @Test
    fun `setfixed refuses an amount outside the configured power range`() {
        givenTarget(MfPlayer(targetId, power = 3.0, powerAtLogout = 3.0))

        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "25"))
        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "-1"))
        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "NaN"))

        verify(sender).sendMessage("${RED}CommandPowerSetInvalidPowerTooHigh 20")
        verify(sender).sendMessage("${RED}CommandPowerSetInvalidPowerTooLow 0")
        verify(sender).sendMessage("${RED}CommandPowerSetInvalidPowerMustBeNumber")
        verify(playerService, never()).update(anyPlayer(), anyTransform())
    }

    @Test
    fun `setfixed without an amount shows the usage`() {
        MfPowerSetFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred"))

        verify(sender).sendMessage("${RED}CommandPowerSetFixedUsage")
    }

    @Test
    fun `clearfixed removes the fixed amount and keeps it as the current power`() {
        givenTarget(MfPlayer(targetId, power = 12.0, powerAtLogout = 3.0, fixedPower = 12.0))

        MfPowerClearFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred"))

        assertNull(saved?.fixedPower)
        assertEquals(12.0, saved?.power)
        assertEquals(12.0, saved?.powerAtLogout)
        verify(sender).sendMessage("${GREEN}CommandPowerClearFixedSuccess Alfred 12")
    }

    @Test
    fun `clearfixed on a player whose power is not fixed changes nothing`() {
        givenTarget(MfPlayer(targetId, power = 12.0, powerAtLogout = 12.0))

        MfPowerClearFixedCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred"))

        verify(sender).sendMessage("${RED}CommandPowerClearFixedNotFixed Alfred")
        verify(playerService, never()).update(anyPlayer(), anyTransform())
    }

    @Test
    fun `power set refuses a player whose power is fixed`() {
        `when`(sender.hasPermission("mf.power.set")).thenReturn(true)
        givenTarget(MfPlayer(targetId, power = 12.0, powerAtLogout = 12.0, fixedPower = 12.0))

        MfPowerSetCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "5"))

        verify(sender).sendMessage("${RED}CommandPowerSetTargetPowerFixed Alfred 12")
        verify(playerService, never()).update(anyPlayer(), anyTransform())
    }

    @Test
    fun `power set still works for a player whose power is not fixed`() {
        `when`(sender.hasPermission("mf.power.set")).thenReturn(true)
        givenTarget(MfPlayer(targetId, power = 12.0, powerAtLogout = 12.0))

        MfPowerSetCommand(plugin).onCommand(sender, command, "power", arrayOf("Alfred", "5"))

        assertEquals(5.0, saved?.power)
    }

    private fun eqPlayer(player: MfPlayer): MfPlayer = eq(player) ?: player

    private fun anyPlayer(): MfPlayer = any(MfPlayer::class.java) ?: MfPlayer(targetId)

    private fun <T> anyTransform(): T {
        any<Any>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
