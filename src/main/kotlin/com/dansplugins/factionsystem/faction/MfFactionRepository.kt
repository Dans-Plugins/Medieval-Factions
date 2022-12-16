package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.player.MfPlayerId

interface MfFactionRepository {

    fun getFaction(id: MfFactionId): MfFaction?
    fun getFaction(name: String): MfFaction?
    fun getFaction(playerId: MfPlayerId): MfFaction?
    fun getFactions(): List<MfFaction>
    fun upsert(faction: MfFaction): MfFaction
    fun delete(factionId: MfFactionId)
}
