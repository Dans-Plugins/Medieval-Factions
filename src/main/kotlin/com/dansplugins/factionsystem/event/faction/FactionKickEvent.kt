package com.dansplugins.factionsystem.event.faction

import com.dansplugins.factionsystem.event.player.PlayerEvent
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class FactionKickEvent(
    @get:JvmName("getFactionId")
    override val factionId: MfFactionId,
    @get:JvmName("getPlayerId")
    override val playerId: MfPlayerId,
    isAsync: Boolean
) : Event(isAsync), FactionEvent, PlayerEvent, Cancellable {

    companion object {
        @JvmStatic private val handlers: HandlerList = HandlerList()

        @JvmStatic fun getHandlerList() = handlers
    }

    private var cancel: Boolean = false

    override fun getHandlers(): HandlerList = getHandlerList()

    override fun isCancelled(): Boolean {
        return cancel
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }
}
