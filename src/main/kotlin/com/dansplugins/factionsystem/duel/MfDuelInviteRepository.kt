package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.player.MfPlayerId

interface MfDuelInviteRepository {
    fun getInvite(inviter: MfPlayerId, invitee: MfPlayerId): MfDuelInvite?
    fun getInvites(): List<MfDuelInvite>
    fun upsert(invite: MfDuelInvite): MfDuelInvite
    fun deleteInvite(inviter: MfPlayerId, invitee: MfPlayerId)
}
