package com.dansplugins.factionsystem.dynmap.helpers

import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerSet

class MarkerSetHelper {

    fun getOrCreateMarkerSet(markerApi: MarkerAPI, setId: String, setName: String): MarkerSet {
        return markerApi.getMarkerSet(setId) ?: markerApi.createMarkerSet(setId, setName, null, false)
    }
}
