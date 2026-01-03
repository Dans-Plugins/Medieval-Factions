package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
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
    fun onAreaEffectCloudApply_BasePotionTypeIsNull_ShouldReturnWithoutError() {
        // Arrange
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionType).thenReturn(null)

        // Act - should not throw NullPointerException
        uut.onAreaEffectCloudApply(event)

        // Assert - event should return early without accessing affected entities
        verify(event, never()).affectedEntities
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionTypeIsNotHarmful_ShouldReturn() {
        // Arrange
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionType = mock(PotionType::class.java)
        
        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionType).thenReturn(potionType)
        // Return an empty list of potion effects (non-harmful)
        `when`(potionType.potionEffects).thenReturn(emptyList())

        // Act
        uut.onAreaEffectCloudApply(event)

        // Assert - should return early without processing affected entities
        verify(event, never()).affectedEntities
    }
}
