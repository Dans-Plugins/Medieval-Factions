package com.dansplugins.factionsystem.command.faction.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale
import java.util.logging.Logger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionPowerCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var claimService: MfClaimService
    private lateinit var language: Language
    private lateinit var config: FileConfiguration
    private lateinit var uut: MfFactionPowerCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        mockServices()
        mockLanguageSystem()
        mockConfig()
        mockScheduler()
        mockLogger()
        uut = MfFactionPowerCommand(plugin)
    }

    @Test
    fun testOnCommand_senderWithoutPermission() {
        // prepare
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.power")).thenReturn(false)
        `when`(language["CommandFactionPowerNoPermission"]).thenReturn("No permission")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}No permission")
    }

    @Test
    fun testOnCommand_consoleSenderWithoutViewOther() {
        // prepare — a non-player sender without mf.power.view.other cannot resolve a target
        val sender = fixture.sender
        val command = fixture.command
        `when`(sender.hasPermission("mf.power")).thenReturn(true)
        `when`(sender.hasPermission("mf.power.view.other")).thenReturn(false)
        `when`(language["CommandFactionPowerNotAPlayer"]).thenReturn("Not a player")

        // execute
        val result = uut.onCommand(sender, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(sender).sendMessage("${ChatColor.RED}Not a player")
    }

    @Test
    fun testOnCommand_showsFactionClaimsWithCapacityWhenLandLimited() {
        // prepare — a player viewing their own faction with factions.limitLand enabled
        val player = fixture.player
        val command = fixture.command
        val mfPlayer = mock(MfPlayer::class.java)
        val faction = mock(MfFaction::class.java)
        `when`(player.hasPermission("mf.power")).thenReturn(true)
        `when`(player.hasPermission("mf.power.view.other")).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        stubFactionPower(faction, power = 5.0)
        `when`(config.getBoolean("factions.limitLand")).thenReturn(true)
        `when`(claimService.getClaims(faction.id)).thenReturn(
            listOf(mock(MfClaimedChunk::class.java), mock(MfClaimedChunk::class.java), mock(MfClaimedChunk::class.java))
        )
        `when`(language["CommandFactionPowerFactionClaims", "3", "5"]).thenReturn("Faction claims: 3/5")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify — the claims line reports used-of-capacity (capacity = floor(power))
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.GRAY}Faction claims: 3/5")
    }

    @Test
    fun testOnCommand_showsFactionClaimsWithoutCapacityWhenLandUnlimited() {
        // prepare — same flow but factions.limitLand disabled, so no capacity is shown
        val player = fixture.player
        val command = fixture.command
        val mfPlayer = mock(MfPlayer::class.java)
        val faction = mock(MfFaction::class.java)
        `when`(player.hasPermission("mf.power")).thenReturn(true)
        `when`(player.hasPermission("mf.power.view.other")).thenReturn(false)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        stubFactionPower(faction, power = 5.0)
        `when`(config.getBoolean("factions.limitLand")).thenReturn(false)
        `when`(claimService.getClaims(faction.id)).thenReturn(
            listOf(mock(MfClaimedChunk::class.java), mock(MfClaimedChunk::class.java))
        )
        `when`(language["CommandFactionPowerFactionClaimsUnlimited", "2"]).thenReturn("Faction claims: 2")

        // execute
        val result = uut.onCommand(player, command, "label", arrayOf())

        // verify
        assertTrue(result)
        verify(player).sendMessage("${ChatColor.GRAY}Faction claims: 2")
    }

    // Helper functions

    private fun stubFactionPower(faction: MfFaction, power: Double) {
        `when`(faction.power).thenReturn(power)
        `when`(faction.maxPower).thenReturn(10.0)
        `when`(faction.memberPower).thenReturn(power)
        `when`(faction.maxMemberPower).thenReturn(10.0)
        `when`(faction.vassalPower).thenReturn(0.0)
        `when`(faction.maxVassalPower).thenReturn(0.0)
        @Suppress("UNCHECKED_CAST")
        val acceptBonusPowerFlag = mock(MfFlag::class.java) as MfFlag<Boolean>
        val flags = mock(MfFlags::class.java)
        val flagValues = mock(MfFlagValues::class.java)
        `when`(plugin.flags).thenReturn(flags)
        `when`(flags.acceptBonusPower).thenReturn(acceptBonusPowerFlag)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues[acceptBonusPowerFlag]).thenReturn(false)
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
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }

    private fun mockConfig() {
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    private fun mockScheduler() {
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)

        val scheduler = mock(BukkitScheduler::class.java)
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
