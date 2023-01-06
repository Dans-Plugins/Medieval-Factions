package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_LAW
import org.jooq.DSLContext

class JooqMfLawRepository(
    private val dsl: DSLContext
) : MfLawRepository {
    override fun getLaw(id: MfLawId): MfLaw? =
        dsl.selectFrom(MF_LAW)
            .where(MF_LAW.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()

    override fun getLaw(factionId: MfFactionId, index: Int?): MfLaw? =
        dsl.selectFrom(MF_LAW)
            .where(MF_LAW.FACTION_ID.eq(factionId.value))
            .and(MF_LAW.NUMBER.eq(index))
            .fetchOne()
            ?.toDomain()

    override fun getLaws(factionId: MfFactionId): List<MfLaw> =
        dsl.selectFrom(MF_LAW)
            .where(MF_LAW.FACTION_ID.eq(factionId.value))
            .orderBy(MF_LAW.NUMBER)
            .fetch()
            .map { it.toDomain() }

    override fun upsert(law: MfLaw): MfLaw {
        val existingLaws = dsl.fetchCount(dsl.selectFrom(MF_LAW).where(MF_LAW.FACTION_ID.eq(law.factionId.value)))
        val rowCount = dsl.insertInto(MF_LAW)
            .set(MF_LAW.ID, law.id.value)
            .set(MF_LAW.VERSION, 1)
            .set(MF_LAW.FACTION_ID, law.factionId.value)
            .set(MF_LAW.TEXT, law.text)
            .set(MF_LAW.NUMBER, existingLaws + 1)
            .onConflict(MF_LAW.ID).doUpdate()
            .set(MF_LAW.VERSION, law.version + 1)
            .set(MF_LAW.FACTION_ID, law.factionId.value)
            .set(MF_LAW.TEXT, law.text)
            .where(MF_LAW.ID.eq(law.id.value))
            .and(MF_LAW.VERSION.eq(law.version))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${law.version}")
        return getLaw(law.id).let(::requireNotNull)
    }

    override fun delete(id: MfLawId) {
        delete(getLaw(id) ?: return)
    }

    override fun delete(law: MfLaw) {
        dsl.deleteFrom(MF_LAW)
            .where(MF_LAW.ID.eq(law.id.value))
            .execute()
        dsl.update(MF_LAW)
            .set(MF_LAW.NUMBER, MF_LAW.NUMBER - 1)
            .where(MF_LAW.FACTION_ID.eq(law.factionId.value))
            .and(MF_LAW.NUMBER.gt(law.number))
    }

    override fun move(law: MfLaw, number: Int) {
        if (law.number == null) return
        if (law.number > number) {
            dsl.update(MF_LAW)
                .set(MF_LAW.NUMBER, (MF_LAW.NUMBER + 1))
                .where(MF_LAW.FACTION_ID.eq(law.factionId.value))
                .and(MF_LAW.NUMBER.lt(law.number))
                .and(MF_LAW.NUMBER.ge(number))
                .execute()
        }
        if (law.number < number) {
            dsl.update(MF_LAW)
                .set(MF_LAW.NUMBER, (MF_LAW.NUMBER - 1))
                .where(MF_LAW.FACTION_ID.eq(law.factionId.value))
                .and(MF_LAW.NUMBER.gt(law.number))
                .and(MF_LAW.NUMBER.le(number))
                .execute()
        }
        dsl.update(MF_LAW)
            .set(MF_LAW.NUMBER, number)
            .where(MF_LAW.FACTION_ID.eq(law.factionId.value))
            .and(MF_LAW.ID.eq(law.id.value))
            .execute()
    }

    private fun com.dansplugins.factionsystem.jooq.tables.records.MfLawRecord.toDomain() = MfLaw(
        MfLawId(id),
        version,
        MfFactionId(factionId),
        text,
        number
    )
}
