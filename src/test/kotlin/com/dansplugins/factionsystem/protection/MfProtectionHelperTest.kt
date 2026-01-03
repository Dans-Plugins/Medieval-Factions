package com.dansplugins.factionsystem.protection

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.MetadataValue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MfProtectionHelperTest {

    @Test
    fun `test event metadata system`() {
        // Test marking and checking if an event is processed
        val mockPlugin = mock(MedievalFactions::class.java)
        val mockPlayer = mock(Player::class.java)
        val mockEvent = mock(PlayerInteractEvent::class.java)
        `when`(mockEvent.player).thenReturn(mockPlayer)

        // Verify the event isn't processed initially
        `when`(mockPlayer.hasMetadata(any())).thenReturn(false)
        assert(!MfProtectionHelper.isEventProcessed(mockEvent))

        // Verify setting metadata properly
        MfProtectionHelper.markEventProcessed(mockPlugin, mockEvent)
        verify(mockPlayer).setMetadata(eq("mf_protection_processed"), any(FixedMetadataValue::class.java))

        // Mock the metadata return value
        val metadataList = listOf<MetadataValue>(FixedMetadataValue(mockPlugin, System.identityHashCode(mockEvent)))
        `when`(mockPlayer.getMetadata("mf_protection_processed")).thenReturn(metadataList)
        `when`(mockPlayer.hasMetadata("mf_protection_processed")).thenReturn(true)

        // Verify the event is now processed
        assert(MfProtectionHelper.isEventProcessed(mockEvent))
    }

    @Test
    fun `test event metadata system with different events`() {
        // Test that different event instances are tracked separately
        val mockPlugin = mock(MedievalFactions::class.java)
        val mockPlayer = mock(Player::class.java)
        val mockEvent1 = mock(PlayerInteractEvent::class.java)
        val mockEvent2 = mock(PlayerInteractEvent::class.java)

        `when`(mockEvent1.player).thenReturn(mockPlayer)
        `when`(mockEvent2.player).thenReturn(mockPlayer)

        // Set up metadata for first event
        MfProtectionHelper.markEventProcessed(mockPlugin, mockEvent1)
        val metadataList = listOf<MetadataValue>(FixedMetadataValue(mockPlugin, System.identityHashCode(mockEvent1)))
        `when`(mockPlayer.getMetadata("mf_protection_processed")).thenReturn(metadataList)
        `when`(mockPlayer.hasMetadata("mf_protection_processed")).thenReturn(true)

        // Event1 should be processed but event2 should not
        assert(MfProtectionHelper.isEventProcessed(mockEvent1))
        assert(!MfProtectionHelper.isEventProcessed(mockEvent2))
    }
}