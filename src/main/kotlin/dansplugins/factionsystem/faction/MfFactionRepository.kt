package dansplugins.factionsystem.faction

import dansplugins.factionsystem.player.MfPlayerId

interface MfFactionRepository {

    fun getFaction(id: MfFactionId): MfFaction?
    fun getFaction(name: String): MfFaction?
    fun getFaction(playerId: MfPlayerId): MfFaction?
    fun upsert(faction: MfFaction): MfFaction

}