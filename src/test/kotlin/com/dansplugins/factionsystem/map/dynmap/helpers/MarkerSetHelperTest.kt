package com.dansplugins.factionsystem.map.dynmap.helpers

import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MarkerSetHelperTest {
    private val markerApi = mock(MarkerAPI::class.java)
    private val uut = MarkerSetHelper()

    @Test
    fun getOrCreateMarkerSet_ShouldReturnExistingMarkerSet() {
        val existingMarkerSet = mock(MarkerSet::class.java)
        `when`(markerApi.getMarkerSet("claims")).thenReturn(existingMarkerSet)

        val result = uut.getOrCreateMarkerSet(markerApi, "claims", "Claims")

        assertEquals(existingMarkerSet, result)
        verify(markerApi, never()).createMarkerSet(anyString(), anyString(), isNull(), anyBoolean())
    }

    @Test
    fun getOrCreateMarkerSet_ShouldCreateNewMarkerSet() {
        `when`(markerApi.getMarkerSet("claims")).thenReturn(null)
        val newMarkerSet = mock(MarkerSet::class.java)
        `when`(markerApi.createMarkerSet("claims", "Claims", null, false)).thenReturn(newMarkerSet)

        val result = uut.getOrCreateMarkerSet(markerApi, "claims", "Claims")

        assertEquals(newMarkerSet, result)
        verify(markerApi).createMarkerSet("claims", "Claims", null, false)
    }
}
