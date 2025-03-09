package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.faction.MfFaction

interface MapService {
    fun scheduleUpdateClaims(faction: MfFaction)
}
