package com.dansplugins.factionsystem.event.faction

import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class FactionClaimEvent(
    @get:JvmName("getFactionId")
    override val factionId: MfFactionId,
    val claim: MfClaimedChunk,
    isAsync: Boolean
) : Event(isAsync), FactionEvent, Cancellable {

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
