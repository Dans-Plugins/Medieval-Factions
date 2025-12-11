package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AreaEffectCloudApplyListenerTest {

    private lateinit var medievalFactions: MedievalFactions
    private lateinit var uut: AreaEffectCloudApplyListener

    @BeforeEach
    fun setUp() {
        medievalFactions = mock(MedievalFactions::class.java)
        uut = AreaEffectCloudApplyListener(medievalFactions)
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNull_ShouldReturnWithoutError() {
        // Arrange
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val affectedEntities = mutableListOf<Player>()
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        
        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(event.affectedEntities).thenReturn(affectedEntities)
        `when`(areaEffectCloud.basePotionData).thenReturn(null)

        // Act - should not throw NullPointerException
        uut.onAreaEffectCloudApply(event)

        // Assert - event should return early without accessing affected entities
        verify(event, never()).affectedEntities
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNotHarmful_ShouldReturn() {
        // Arrange
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val affectedEntities = mutableListOf<Player>()
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionData = mock(PotionData::class.java)
        val potionType = mock(PotionType::class.java)
        
        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(event.affectedEntities).thenReturn(affectedEntities)
        `when`(areaEffectCloud.basePotionData).thenReturn(potionData)
        `when`(potionData.type).thenReturn(potionType)
        // Return a non-harmful effect type (e.g., SPEED or REGENERATION)
        `when`(potionType.effectType).thenReturn(PotionEffectType.SPEED)

        // Act
        uut.onAreaEffectCloudApply(event)

        // Assert - should return early without processing affected entities
        verify(event, never()).affectedEntities
    }
}
