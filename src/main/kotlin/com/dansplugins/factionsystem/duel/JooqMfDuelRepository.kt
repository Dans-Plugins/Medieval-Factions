package com.dansplugins.factionsystem.duel

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_DUEL
import com.dansplugins.factionsystem.jooq.tables.records.MfDuelRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class JooqMfDuelRepository(private val dsl: DSLContext) : MfDuelRepository {
    override fun getDuels(): List<MfDuel> =
        dsl.selectFrom(MF_DUEL)
            .fetch()
            .map { it.toDomain() }

    private fun getDuel(id: MfDuelId): MfDuel? =
        dsl.selectFrom(MF_DUEL)
            .where(MF_DUEL.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()

    override fun upsert(duel: MfDuel): MfDuel {
        val rowCount = dsl.insertInto(MF_DUEL)
            .set(MF_DUEL.ID, duel.id.value)
            .set(MF_DUEL.VERSION, 1)
            .set(MF_DUEL.CHALLENGER_ID, duel.challengerId.value)
            .set(MF_DUEL.CHALLENGED_ID, duel.challengedId.value)
            .set(MF_DUEL.CHALLENGER_HEALTH, duel.challengerHealth)
            .set(MF_DUEL.CHALLENGED_HEALTH, duel.challengedHealth)
            .set(MF_DUEL.END_TIME, LocalDateTime.ofInstant(duel.endTime, UTC))
            .onConflict(MF_DUEL.ID).doUpdate()
            .set(MF_DUEL.CHALLENGER_ID, duel.challengerId.value)
            .set(MF_DUEL.CHALLENGED_ID, duel.challengedId.value)
            .set(MF_DUEL.CHALLENGER_HEALTH, duel.challengerHealth)
            .set(MF_DUEL.CHALLENGED_HEALTH, duel.challengedHealth)
            .set(MF_DUEL.END_TIME, LocalDateTime.ofInstant(duel.endTime, UTC))
            .set(MF_DUEL.VERSION, duel.version + 1)
            .where(MF_DUEL.ID.eq(duel.id.value))
            .and(MF_DUEL.VERSION.eq(duel.version))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${duel.version}")
        return getDuel(duel.id).let(::requireNotNull)
    }

    override fun delete(id: MfDuelId) {
        dsl.deleteFrom(MF_DUEL)
            .where(MF_DUEL.ID.eq(id.value))
            .execute()
    }

    private fun MfDuelRecord.toDomain() = MfDuel(
        id.let(::MfDuelId),
        version,
        challengerId.let(::MfPlayerId),
        challengedId.let(::MfPlayerId),
        challengerHealth,
        challengedHealth,
        endTime.toInstant(UTC)
    )
}