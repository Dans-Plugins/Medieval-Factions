package com.dansplugins.factionsystem.relationship.rival

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION_RIVAL
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionRivalRecord
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class JooqMfFactionRivalRepository(private val dsl: DSLContext) : MfFactionRivalRepository {

    override fun getRivals(): List<MfFactionRival> =
        dsl.selectFrom(MF_FACTION_RIVAL).fetch().map { it.toDomain() }

    override fun insert(rival: MfFactionRival): MfFactionRival {
        dsl.insertInto(MF_FACTION_RIVAL)
            .set(MF_FACTION_RIVAL.FACTION_ID, rival.factionId.value)
            .set(MF_FACTION_RIVAL.TARGET_ID, rival.targetId.value)
            .set(MF_FACTION_RIVAL.CREATED_AT, LocalDateTime.ofInstant(rival.createdAt, UTC))
            .onConflict(MF_FACTION_RIVAL.FACTION_ID, MF_FACTION_RIVAL.TARGET_ID).doNothing()
            .execute()
        return dsl.selectFrom(MF_FACTION_RIVAL)
            .where(MF_FACTION_RIVAL.FACTION_ID.eq(rival.factionId.value))
            .and(MF_FACTION_RIVAL.TARGET_ID.eq(rival.targetId.value))
            .fetchOne()
            ?.toDomain()
            ?: rival
    }

    override fun delete(factionId: MfFactionId, targetId: MfFactionId) {
        dsl.deleteFrom(MF_FACTION_RIVAL)
            .where(MF_FACTION_RIVAL.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_RIVAL.TARGET_ID.eq(targetId.value))
            .execute()
    }

    private fun MfFactionRivalRecord.toDomain() = MfFactionRival(
        factionId = MfFactionId(factionId),
        targetId = MfFactionId(targetId),
        createdAt = createdAt.toInstant(UTC)
    )
}
