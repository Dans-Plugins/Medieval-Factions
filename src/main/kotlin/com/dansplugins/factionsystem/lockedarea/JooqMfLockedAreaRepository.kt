package com.dansplugins.factionsystem.lockedarea

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.db.MfVersionedWrite
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_LOCKED_AREA
import com.dansplugins.factionsystem.jooq.Tables.MF_LOCKED_AREA_ACCESSOR
import com.dansplugins.factionsystem.jooq.Tables.MF_LOCKED_AREA_ROLE_ACCESSOR
import com.dansplugins.factionsystem.jooq.tables.records.MfLockedAreaRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext
import java.util.UUID

class JooqMfLockedAreaRepository(private val dsl: DSLContext) : MfLockedAreaRepository {

    override fun getLockedAreas(): List<MfLockedArea> {
        val accessors = dsl.selectFrom(MF_LOCKED_AREA_ACCESSOR).fetch()
            .groupBy({ it.lockedAreaId }, { it.playerId.let(::MfPlayerId) })
        val roleAccessors = dsl.selectFrom(MF_LOCKED_AREA_ROLE_ACCESSOR).fetch()
            .groupBy({ it.lockedAreaId }, { it.roleId.let(::MfFactionRoleId) })
        return dsl.selectFrom(MF_LOCKED_AREA).fetch()
            .map { it.toDomain(accessors[it.id].orEmpty(), roleAccessors[it.id].orEmpty()) }
    }

    override fun upsert(lockedArea: MfLockedArea): MfLockedArea {
        // A stale write is signalled with null so jOOQ does not wrap the exception (see JooqMfLockRepository).
        return dsl.transactionResult { config ->
            val transactionalDsl = config.dsl()
            if (!write(transactionalDsl, lockedArea)) return@transactionResult null
            val id = lockedArea.id.value
            transactionalDsl.deleteFrom(MF_LOCKED_AREA_ACCESSOR).where(MF_LOCKED_AREA_ACCESSOR.LOCKED_AREA_ID.eq(id)).execute()
            transactionalDsl.deleteFrom(MF_LOCKED_AREA_ROLE_ACCESSOR).where(MF_LOCKED_AREA_ROLE_ACCESSOR.LOCKED_AREA_ID.eq(id)).execute()
            val accessors = lockedArea.accessors.distinctBy { it.value }
            val roleAccessors = lockedArea.accessorRoleIds.distinctBy { it.value }
            accessors.forEach {
                transactionalDsl.insertInto(MF_LOCKED_AREA_ACCESSOR)
                    .set(MF_LOCKED_AREA_ACCESSOR.LOCKED_AREA_ID, id)
                    .set(MF_LOCKED_AREA_ACCESSOR.PLAYER_ID, it.value)
                    .execute()
            }
            roleAccessors.forEach {
                transactionalDsl.insertInto(MF_LOCKED_AREA_ROLE_ACCESSOR)
                    .set(MF_LOCKED_AREA_ROLE_ACCESSOR.LOCKED_AREA_ID, id)
                    .set(MF_LOCKED_AREA_ROLE_ACCESSOR.ROLE_ID, it.value)
                    .execute()
            }
            transactionalDsl.selectFrom(MF_LOCKED_AREA).where(MF_LOCKED_AREA.ID.eq(id)).fetchOne()
                .let(::requireNotNull)
                .toDomain(accessors, roleAccessors)
        } ?: throw OptimisticLockingFailureException("Invalid version: ${lockedArea.version}")
    }

    private fun write(dsl: DSLContext, lockedArea: MfLockedArea): Boolean {
        val min = lockedArea.area.minPosition
        val max = lockedArea.area.maxPosition
        return MfVersionedWrite.write(
            update = {
                dsl.update(MF_LOCKED_AREA)
                    .set(MF_LOCKED_AREA.NAME, lockedArea.name)
                    .set(MF_LOCKED_AREA.VERSION, lockedArea.version + 1)
                    .where(MF_LOCKED_AREA.ID.eq(lockedArea.id.value))
                    .and(MF_LOCKED_AREA.VERSION.eq(lockedArea.version))
                    .execute()
            },
            rowExists = {
                dsl.select(MF_LOCKED_AREA.ID).from(MF_LOCKED_AREA).where(MF_LOCKED_AREA.ID.eq(lockedArea.id.value)).forUpdate().fetchOne() != null
            },
            insert = {
                dsl.insertInto(MF_LOCKED_AREA)
                    .set(MF_LOCKED_AREA.ID, lockedArea.id.value)
                    .set(MF_LOCKED_AREA.VERSION, 1)
                    .set(MF_LOCKED_AREA.FACTION_ID, lockedArea.factionId.value)
                    .set(MF_LOCKED_AREA.NAME, lockedArea.name)
                    .set(MF_LOCKED_AREA.WORLD_ID, min.worldId.toString())
                    .set(MF_LOCKED_AREA.MIN_X, min.x)
                    .set(MF_LOCKED_AREA.MIN_Y, min.y)
                    .set(MF_LOCKED_AREA.MIN_Z, min.z)
                    .set(MF_LOCKED_AREA.MAX_X, max.x)
                    .set(MF_LOCKED_AREA.MAX_Y, max.y)
                    .set(MF_LOCKED_AREA.MAX_Z, max.z)
                    .execute()
            }
        )
    }

    override fun delete(id: MfLockedAreaId) {
        dsl.deleteFrom(MF_LOCKED_AREA).where(MF_LOCKED_AREA.ID.eq(id.value)).execute()
    }

    private fun MfLockedAreaRecord.toDomain(accessors: List<MfPlayerId>, roleAccessors: List<MfFactionRoleId>): MfLockedArea {
        val world = UUID.fromString(worldId)
        return MfLockedArea(
            id = id.let(::MfLockedAreaId),
            version = version,
            factionId = factionId.let(::MfFactionId),
            name = name,
            area = MfCuboidArea(MfBlockPosition(world, minX, minY, minZ), MfBlockPosition(world, maxX, maxY, maxZ)),
            accessors = accessors,
            accessorRoleIds = roleAccessors
        )
    }
}
