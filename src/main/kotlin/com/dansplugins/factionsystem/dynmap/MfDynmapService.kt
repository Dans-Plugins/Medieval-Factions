package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.faction.MfFaction

interface MfDynmapService {
    fun scheduleUpdateClaims(faction: MfFaction)
}
