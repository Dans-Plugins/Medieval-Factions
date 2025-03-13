package com.dansplugins.factionsystem.map

import com.dansplugins.factionsystem.faction.MfFaction

/**
 * Interface representing a map service for scheduling updates to faction claims.
 */
interface MapService {
    /**
     * Schedules an update for the claims of the specified faction.
     *
     * @param faction The faction whose claims need to be updated.
     */
    fun scheduleUpdateClaims(faction: MfFaction)
}
