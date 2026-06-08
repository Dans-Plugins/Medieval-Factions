package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.potion.PotionData
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
        uut.harmfulPotionEffectTypeNames = emptyList()
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNull_ShouldReturnWithoutError() {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)

        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(null)

        uut.onAreaEffectCloudApply(event)

        verify(event, never()).affectedEntities
    }

    @Test
    fun onAreaEffectCloudApply_BasePotionDataIsNotHarmful_ShouldReturn() {
        val areaEffectCloud = mock(AreaEffectCloud::class.java)
        val event = mock(AreaEffectCloudApplyEvent::class.java)
        val potionData = mock(PotionData::class.java)
        val potionType = mock(PotionType::class.java)

        `when`(event.entity).thenReturn(areaEffectCloud)
        `when`(areaEffectCloud.basePotionData).thenReturn(potionData)
        `when`(potionData.type).thenReturn(potionType)
        `when`(potionType.effectType).thenReturn(null)

        uut.onAreaEffectCloudApply(event)

        verify(event, never()).affectedEntities
    }
}
