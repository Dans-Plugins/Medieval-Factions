package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.jooq.Tables.MF_CLAIMED_CHUNK
import com.dansplugins.factionsystem.jooq.tables.records.MfClaimedChunkRecord
import org.jooq.DSLContext
import java.util.*

class JooqMfClaimedChunkRepository(private val dsl: DSLContext) : MfClaimedChunkRepository {
    override fun getClaim(worldId: UUID, x: Int, z: Int): MfClaimedChunk? {
        return dsl.selectFrom(MF_CLAIMED_CHUNK)
            .where(MF_CLAIMED_CHUNK.WORLD_ID.eq(worldId.toString()))
            .and(MF_CLAIMED_CHUNK.X.eq(x))
            .and(MF_CLAIMED_CHUNK.Z.eq(z))
            .fetchOne()
            ?.toDomain()
    }

    override fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> {
        return dsl.selectFrom(MF_CLAIMED_CHUNK)
            .where(MF_CLAIMED_CHUNK.FACTION_ID.eq(factionId.value))
            .fetch()
            .map { it.toDomain() }
    }

    override fun getClaims(): List<MfClaimedChunk> {
        return dsl.selectFrom(MF_CLAIMED_CHUNK)
            .fetch()
            .map { it.toDomain() }
    }

    override fun upsert(claim: MfClaimedChunk): MfClaimedChunk {
        dsl.insertInto(MF_CLAIMED_CHUNK)
            .set(MF_CLAIMED_CHUNK.WORLD_ID, claim.worldId.toString())
            .set(MF_CLAIMED_CHUNK.X, claim.x)
            .set(MF_CLAIMED_CHUNK.Z, claim.z)
            .set(MF_CLAIMED_CHUNK.FACTION_ID, claim.factionId.value)
            .onConflict(MF_CLAIMED_CHUNK.WORLD_ID, MF_CLAIMED_CHUNK.X, MF_CLAIMED_CHUNK.Z).doUpdate()
            .set(MF_CLAIMED_CHUNK.FACTION_ID, claim.factionId.value)
            .where(MF_CLAIMED_CHUNK.WORLD_ID.eq(claim.worldId.toString()))
            .and(MF_CLAIMED_CHUNK.X.eq(claim.x))
            .and(MF_CLAIMED_CHUNK.Z.eq(claim.z))
            .execute()
        return dsl.selectFrom(MF_CLAIMED_CHUNK)
            .where(MF_CLAIMED_CHUNK.WORLD_ID.eq(claim.worldId.toString()))
            .and(MF_CLAIMED_CHUNK.X.eq(claim.x))
            .and(MF_CLAIMED_CHUNK.Z.eq(claim.z))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    override fun delete(worldId: UUID, x: Int, z: Int) {
        dsl.deleteFrom(MF_CLAIMED_CHUNK)
            .where(MF_CLAIMED_CHUNK.WORLD_ID.eq(worldId.toString()))
            .and(MF_CLAIMED_CHUNK.X.eq(x))
            .and(MF_CLAIMED_CHUNK.Z.eq(z))
            .execute()
    }

    override fun deleteAll(factionId: MfFactionId) {
        dsl.deleteFrom(MF_CLAIMED_CHUNK)
            .where(MF_CLAIMED_CHUNK.FACTION_ID.eq(factionId.value))
            .execute()
    }

    private fun MfClaimedChunkRecord.toDomain() = MfClaimedChunk(
        worldId.let(UUID::fromString),
        x,
        z,
        factionId.let(::MfFactionId)
    )
}
