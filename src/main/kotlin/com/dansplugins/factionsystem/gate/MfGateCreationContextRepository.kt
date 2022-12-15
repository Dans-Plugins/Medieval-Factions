package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.player.MfPlayerId

interface MfGateCreationContextRepository {

    fun getContext(playerId: MfPlayerId): MfGateCreationContext?
    fun upsert(context: MfGateCreationContext): MfGateCreationContext
    fun delete(playerId: MfPlayerId)
}
