package com.dansplugins.factionsystem.map.dynmap.helpers

import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerSet

/**
 * Helper class for managing MarkerSets in Dynmap.
 */
class MarkerSetHelper {

    /**
     * Retrieves an existing MarkerSet by its ID or creates a new one if it does not exist.
     *
     * @param markerApi The MarkerAPI instance used to interact with Dynmap markers.
     * @param setId The ID of the MarkerSet to retrieve or create.
     * @param setName The name of the MarkerSet to create if it does not exist.
     * @return The existing or newly created MarkerSet.
     */
    fun getOrCreateMarkerSet(markerApi: MarkerAPI, setId: String, setName: String): MarkerSet {
        return markerApi.getMarkerSet(setId) ?: markerApi.createMarkerSet(setId, setName, null, false)
    }
}
