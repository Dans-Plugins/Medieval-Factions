package com.dansplugins.factionsystem.interaction

import com.dansplugins.factionsystem.jooq.Tables.MF_PLAYER_INTERACTION_STATUS
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext

class JooqMfInteractionStatusRepository(private val dsl: DSLContext) : MfInteractionStatusRepository {
    override fun getInteractionStatus(playerId: MfPlayerId) =
        dsl.selectFrom(MF_PLAYER_INTERACTION_STATUS)
            .where(MF_PLAYER_INTERACTION_STATUS.PLAYER_ID.eq(playerId.value))
            .fetchOne()
            ?.let { result -> result.interactionStatus?.let(MfInteractionStatus::valueOf) }

    override fun setInteractionStatus(playerId: MfPlayerId, status: MfInteractionStatus?) {
        dsl.insertInto(MF_PLAYER_INTERACTION_STATUS)
            .set(MF_PLAYER_INTERACTION_STATUS.PLAYER_ID, playerId.value)
            .set(MF_PLAYER_INTERACTION_STATUS.INTERACTION_STATUS, status?.name)
            .onConflict(MF_PLAYER_INTERACTION_STATUS.PLAYER_ID).doUpdate()
            .set(MF_PLAYER_INTERACTION_STATUS.INTERACTION_STATUS, status?.name)
            .where(MF_PLAYER_INTERACTION_STATUS.PLAYER_ID.eq(playerId.value))
            .execute()
    }
}
