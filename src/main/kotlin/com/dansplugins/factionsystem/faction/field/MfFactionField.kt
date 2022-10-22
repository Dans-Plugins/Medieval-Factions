package com.dansplugins.factionsystem.faction.field

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import net.md_5.bungee.api.chat.BaseComponent

abstract class MfFactionField(val name: String) {
    abstract fun isVisibleFor(factionId: MfFactionId, playerId: MfPlayerId): Boolean
    abstract fun get(factionId: MfFactionId): Array<BaseComponent>
}