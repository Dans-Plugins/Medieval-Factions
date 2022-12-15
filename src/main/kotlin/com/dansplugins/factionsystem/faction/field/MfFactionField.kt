package com.dansplugins.factionsystem.faction.field

import net.md_5.bungee.api.chat.BaseComponent

abstract class MfFactionField(val name: String) {
    abstract fun isVisibleFor(factionId: String, playerId: String): Boolean
    abstract fun get(factionId: String): Array<BaseComponent>
}
