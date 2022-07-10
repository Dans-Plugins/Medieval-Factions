package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.jooq.Tables.MF_PLAYER
import com.dansplugins.factionsystem.jooq.tables.records.MfPlayerRecord
import org.jooq.DSLContext

class JooqMfPlayerRepository(private val dsl: DSLContext) : MfPlayerRepository {
    override fun getPlayer(id: MfPlayerId) =
        dsl.selectFrom(MF_PLAYER)
            .where(MF_PLAYER.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()

    override fun upsert(player: MfPlayer): MfPlayer {
        dsl.insertInto(MF_PLAYER)
            .set(MF_PLAYER.ID, player.id.value)
            .onConflict().doNothing()
            .execute()
        return getPlayer(player.id).let(::requireNotNull)
    }

    private fun com.dansplugins.factionsystem.jooq.tables.records.MfPlayerRecord.toDomain() = MfPlayer(MfPlayerId(id))
}