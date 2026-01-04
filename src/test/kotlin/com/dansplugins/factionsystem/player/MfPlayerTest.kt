package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MfPlayerTest {

    @Test
    fun testInitializationWithAllParameters() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val version = 1
        val name = "TestPlayer"
        val power = 10.0
        val powerAtLogout = 8.0
        val isBypassEnabled = true
        val chatChannel = MfFactionChatChannel.FACTION

        // execute
        val player = MfPlayer(
            id = id,
            version = version,
            name = name,
            power = power,
            powerAtLogout = powerAtLogout,
            isBypassEnabled = isBypassEnabled,
            chatChannel = chatChannel
        )

        // verify
        assertEquals(id, player.id)
        assertEquals(version, player.version)
        assertEquals(name, player.name)
        assertEquals(power, player.power)
        assertEquals(powerAtLogout, player.powerAtLogout)
        assertTrue(player.isBypassEnabled)
        assertEquals(chatChannel, player.chatChannel)
    }

    @Test
    fun testDefaultValues() {
        // prepare
        val id = MfPlayerId("test-player-id")

        // execute
        val player = MfPlayer(id = id)

        // verify
        assertEquals(id, player.id)
        assertEquals(0, player.version)
        assertNull(player.name)
        assertEquals(0.0, player.power)
        assertEquals(0.0, player.powerAtLogout)
        assertFalse(player.isBypassEnabled)
        assertNull(player.chatChannel)
    }

    @Test
    fun testPlayerWithPositivePower() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val power = 25.5

        // execute
        val player = MfPlayer(id = id, power = power, powerAtLogout = power)

        // verify
        assertEquals(power, player.power)
        assertEquals(power, player.powerAtLogout)
    }

    @Test
    fun testPlayerWithZeroPower() {
        // prepare
        val id = MfPlayerId("test-player-id")

        // execute
        val player = MfPlayer(id = id, power = 0.0, powerAtLogout = 0.0)

        // verify
        assertEquals(0.0, player.power)
        assertEquals(0.0, player.powerAtLogout)
    }

    @Test
    fun testPlayerWithNegativePower() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val power = -5.0

        // execute
        val player = MfPlayer(id = id, power = power, powerAtLogout = power)

        // verify
        assertEquals(power, player.power)
        assertEquals(power, player.powerAtLogout)
    }

    @Test
    fun testPlayerPowerDifferentFromPowerAtLogout() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val power = 15.0
        val powerAtLogout = 10.0

        // execute
        val player = MfPlayer(id = id, power = power, powerAtLogout = powerAtLogout)

        // verify
        assertEquals(power, player.power)
        assertEquals(powerAtLogout, player.powerAtLogout)
    }

    @Test
    fun testPlayerBypassEnabled() {
        // prepare
        val id = MfPlayerId("test-player-id")

        // execute
        val player = MfPlayer(id = id, isBypassEnabled = true)

        // verify
        assertTrue(player.isBypassEnabled)
    }

    @Test
    fun testPlayerBypassDisabled() {
        // prepare
        val id = MfPlayerId("test-player-id")

        // execute
        val player = MfPlayer(id = id, isBypassEnabled = false)

        // verify
        assertFalse(player.isBypassEnabled)
    }

    @Test
    fun testPlayerWithFactionChatChannel() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val chatChannel = MfFactionChatChannel.FACTION

        // execute
        val player = MfPlayer(id = id, chatChannel = chatChannel)

        // verify
        assertEquals(chatChannel, player.chatChannel)
    }

    @Test
    fun testPlayerWithAlliesChatChannel() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val chatChannel = MfFactionChatChannel.ALLIES

        // execute
        val player = MfPlayer(id = id, chatChannel = chatChannel)

        // verify
        assertEquals(chatChannel, player.chatChannel)
    }

    @Test
    fun testPlayerWithVassalsChatChannel() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val chatChannel = MfFactionChatChannel.VASSALS

        // execute
        val player = MfPlayer(id = id, chatChannel = chatChannel)

        // verify
        assertEquals(chatChannel, player.chatChannel)
    }

    @Test
    fun testPlayerWithNullChatChannel() {
        // prepare
        val id = MfPlayerId("test-player-id")

        // execute
        val player = MfPlayer(id = id, chatChannel = null)

        // verify
        assertNull(player.chatChannel)
    }

    @Test
    fun testPlayerVersioning() {
        // prepare
        val id = MfPlayerId("test-player-id")
        
        // execute
        val playerV0 = MfPlayer(id = id, version = 0)
        val playerV1 = MfPlayer(id = id, version = 1)
        val playerV2 = MfPlayer(id = id, version = 2)

        // verify
        assertEquals(0, playerV0.version)
        assertEquals(1, playerV1.version)
        assertEquals(2, playerV2.version)
    }

    @Test
    fun testCopyWithUpdatedPower() {
        // prepare
        val id = MfPlayerId("test-player-id")
        val originalPlayer = MfPlayer(id = id, power = 10.0, powerAtLogout = 10.0)

        // execute
        val updatedPlayer = originalPlayer.copy(power = 15.0)

        // verify
        assertEquals(15.0, updatedPlayer.power)
        assertEquals(10.0, updatedPlayer.powerAtLogout) // Should remain unchanged
        assertEquals(originalPlayer.id, updatedPlayer.id)
    }
}
