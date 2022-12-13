package com.dansplugins.factionsystem.interaction

import com.dansplugins.factionsystem.player.MfPlayerId

interface MfInteractionStatusRepository {

    fun getInteractionStatus(playerId: MfPlayerId): MfInteractionStatus?
    fun setInteractionStatus(playerId: MfPlayerId, status: MfInteractionStatus?)
}
