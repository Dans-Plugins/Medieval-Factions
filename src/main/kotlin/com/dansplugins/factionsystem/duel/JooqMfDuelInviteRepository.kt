package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.jooq.Tables.MF_DUEL_INVITE
import com.dansplugins.factionsystem.jooq.tables.records.MfDuelInviteRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext

class JooqMfDuelInviteRepository(private val dsl: DSLContext) : MfDuelInviteRepository {
    override fun getInvite(inviter: MfPlayerId, invitee: MfPlayerId): MfDuelInvite? =
        dsl.selectFrom(MF_DUEL_INVITE)
            .where(MF_DUEL_INVITE.INVITER_ID.eq(inviter.value))
            .and(MF_DUEL_INVITE.INVITEE_ID.eq(invitee.value))
            .fetchOne()
            ?.toDomain()

    override fun getInvites(): List<MfDuelInvite> =
        dsl.selectFrom(MF_DUEL_INVITE)
            .fetch()
            .map { it.toDomain() }

    override fun upsert(invite: MfDuelInvite): MfDuelInvite {
        dsl.insertInto(MF_DUEL_INVITE)
            .set(MF_DUEL_INVITE.INVITER_ID, invite.inviterId.value)
            .set(MF_DUEL_INVITE.INVITEE_ID, invite.inviteeId.value)
            .onConflict(MF_DUEL_INVITE.INVITER_ID, MF_DUEL_INVITE.INVITEE_ID).doNothing()
            .execute()
        return getInvite(invite.inviterId, invite.inviteeId).let(::requireNotNull)
    }

    override fun deleteInvite(inviter: MfPlayerId, invitee: MfPlayerId) {
        dsl.deleteFrom(MF_DUEL_INVITE)
            .where(MF_DUEL_INVITE.INVITER_ID.eq(inviter.value))
            .and(MF_DUEL_INVITE.INVITEE_ID.eq(invitee.value))
            .execute()
    }

    private fun MfDuelInviteRecord.toDomain() = MfDuelInvite(
        inviterId.let(::MfPlayerId),
        inviteeId.let(::MfPlayerId)
    )
}
