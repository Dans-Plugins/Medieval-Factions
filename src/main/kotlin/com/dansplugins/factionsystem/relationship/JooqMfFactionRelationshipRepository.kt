package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION_RELATIONSHIP
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionRelationshipRecord
import org.jooq.DSLContext

class JooqMfFactionRelationshipRepository(val dsl: DSLContext) : MfFactionRelationshipRepository {

    override fun getFactionRelationship(relationshipId: MfFactionRelationshipId): MfFactionRelationship? {
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.ID.eq(relationshipId.value))
            .fetchOne()
            ?.toDomain()
    }

    override fun getFactionRelationships(factionId: MfFactionId, targetId: MfFactionId): List<MfFactionRelationship> {
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_RELATIONSHIP.TARGET_ID.eq(targetId.value))
            .fetch()
            .map { it.toDomain() }
    }

    override fun getFactionRelationships(
        factionId: MfFactionId,
        type: MfFactionRelationshipType
    ): List<MfFactionRelationship> {
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_RELATIONSHIP.TYPE.eq(type.name))
            .fetch()
            .map { it.toDomain() }
    }

    override fun getFactionRelationships(factionId: MfFactionId): List<MfFactionRelationship> {
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.FACTION_ID.eq(factionId.value))
            .fetch()
            .map { it.toDomain() }
    }

    override fun getFactionRelationships(): List<MfFactionRelationship> {
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .fetch()
            .map { it.toDomain() }
    }

    override fun upsert(relationship: MfFactionRelationship): MfFactionRelationship {
        dsl.insertInto(MF_FACTION_RELATIONSHIP)
            .set(MF_FACTION_RELATIONSHIP.ID, relationship.id.value)
            .set(MF_FACTION_RELATIONSHIP.FACTION_ID, relationship.factionId.value)
            .set(MF_FACTION_RELATIONSHIP.TARGET_ID, relationship.targetId.value)
            .set(MF_FACTION_RELATIONSHIP.TYPE, relationship.type.name)
            .onConflict(MF_FACTION_RELATIONSHIP.ID).doUpdate()
            .set(MF_FACTION_RELATIONSHIP.FACTION_ID, relationship.factionId.value)
            .set(MF_FACTION_RELATIONSHIP.TARGET_ID, relationship.targetId.value)
            .set(MF_FACTION_RELATIONSHIP.TYPE, relationship.type.name)
            .where(MF_FACTION_RELATIONSHIP.ID.eq(relationship.id.value))
            .execute()
        return dsl.selectFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.ID.eq(relationship.id.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    override fun delete(relationshipId: MfFactionRelationshipId) {
        dsl.deleteFrom(MF_FACTION_RELATIONSHIP)
            .where(MF_FACTION_RELATIONSHIP.ID.eq(relationshipId.value))
            .execute()
    }

    private fun MfFactionRelationshipRecord.toDomain() = MfFactionRelationship(
        id.let(::MfFactionRelationshipId),
        factionId.let(::MfFactionId),
        targetId.let(::MfFactionId),
        MfFactionRelationshipType.valueOf(type)
    )
}
