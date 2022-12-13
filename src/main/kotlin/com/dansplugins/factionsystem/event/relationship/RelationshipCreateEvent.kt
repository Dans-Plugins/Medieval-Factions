package com.dansplugins.factionsystem.event.relationship

import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipId
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class RelationshipCreateEvent(
    @get:JvmName("getRelationshipId")
    override val relationshipId: MfFactionRelationshipId,
    val relationship: MfFactionRelationship,
    isAsync: Boolean
) : Event(isAsync), RelationshipEvent, Cancellable {

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
