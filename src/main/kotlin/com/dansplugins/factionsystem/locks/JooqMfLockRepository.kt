package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.db.MfVersionedWrite
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_LOCKED_BLOCK
import com.dansplugins.factionsystem.jooq.Tables.MF_LOCKED_BLOCK_ACCESSOR
import com.dansplugins.factionsystem.jooq.tables.records.MfLockedBlockRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*
import com.dansplugins.factionsystem.locks.MfLockedBlock as DomainMfLockedBlock

class JooqMfLockRepository(private val dsl: DSLContext) : MfLockRepository {
    override fun getLockedBlock(id: MfLockedBlockId): DomainMfLockedBlock? = getLockedBlock(MF_LOCKED_BLOCK.ID.eq(id.value))

    override fun getLockedBlock(worldId: UUID, x: Int, y: Int, z: Int): DomainMfLockedBlock? =
        getLockedBlock(
            MF_LOCKED_BLOCK.WORLD_ID.eq(worldId.toString())
                .and(MF_LOCKED_BLOCK.X.eq(x))
                .and(MF_LOCKED_BLOCK.Y.eq(y))
                .and(MF_LOCKED_BLOCK.Z.eq(z))
        )

    private fun getLockedBlock(condition: Condition): DomainMfLockedBlock? {
        val results = dsl.selectFrom(
            MF_LOCKED_BLOCK
                .leftJoin(MF_LOCKED_BLOCK_ACCESSOR)
                .on(MF_LOCKED_BLOCK.ID.eq(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID))
        ).where(condition).fetch()
        val lockedBlockRecord = results.into(MF_LOCKED_BLOCK).firstOrNull() ?: return null
        val accessorRecords = results.into(MF_LOCKED_BLOCK_ACCESSOR)
        val accessors = accessorRecords.map { record -> record.playerId.let(::MfPlayerId) }
        return lockedBlockRecord.toDomain(accessors)
    }

    override fun getLockedBlocks(): List<DomainMfLockedBlock> {
        return dsl.selectFrom(
            MF_LOCKED_BLOCK
                .leftJoin(MF_LOCKED_BLOCK_ACCESSOR)
                .on(MF_LOCKED_BLOCK.ID.eq(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID))
        ).fetch()
            .groupBy { it[MF_LOCKED_BLOCK.ID] }
            .map { (_, records) ->
                val accessors = records.filter { it[MF_LOCKED_BLOCK_ACCESSOR.PLAYER_ID] != null }
                    .map { it.into(MF_LOCKED_BLOCK_ACCESSOR).playerId.let(::MfPlayerId) }
                records.first().into(MF_LOCKED_BLOCK).toDomain(accessors)
            }
    }

    override fun upsert(lockedBlock: DomainMfLockedBlock): DomainMfLockedBlock {
        // The conflict is signalled with null rather than thrown inside the transaction: jOOQ
        // wraps a checked exception thrown there in a DataAccessException, which would hide the
        // OptimisticLockingFailureException from MfLockService's CONFLICT mapping. Nothing has
        // been written when null is returned.
        return upsertIfCurrent(lockedBlock) ?: throw OptimisticLockingFailureException("Invalid version: ${lockedBlock.version}")
    }

    private fun upsertIfCurrent(lockedBlock: DomainMfLockedBlock): DomainMfLockedBlock? {
        return dsl.transactionResult { config ->
            val transactionalDsl = config.dsl()
            val newState = upsertLockedBlock(transactionalDsl, lockedBlock) ?: return@transactionResult null

            deleteAccessors(transactionalDsl, lockedBlock.id)
            val newAccessors = lockedBlock.accessors.map { upsertAccessor(transactionalDsl, lockedBlock.id, it) }

            return@transactionResult newState.copy(
                accessors = newAccessors
            )
        }
    }

    /** Writes the locked block row, or returns null without writing if [lockedBlock] is stale. */
    private fun upsertLockedBlock(dsl: DSLContext, lockedBlock: DomainMfLockedBlock): DomainMfLockedBlock? {
        // See MfVersionedWrite for why this is not a single guarded upsert (#2076).
        val written = MfVersionedWrite.write(
            update = {
                dsl.update(MF_LOCKED_BLOCK)
                    .set(MF_LOCKED_BLOCK.WORLD_ID, lockedBlock.block.worldId.toString())
                    .set(MF_LOCKED_BLOCK.X, lockedBlock.block.x)
                    .set(MF_LOCKED_BLOCK.Y, lockedBlock.block.y)
                    .set(MF_LOCKED_BLOCK.Z, lockedBlock.block.z)
                    .set(MF_LOCKED_BLOCK.CHUNK_X, lockedBlock.chunkX)
                    .set(MF_LOCKED_BLOCK.CHUNK_Z, lockedBlock.chunkZ)
                    .set(MF_LOCKED_BLOCK.PLAYER_ID, lockedBlock.playerId.value)
                    .set(MF_LOCKED_BLOCK.VERSION, lockedBlock.version + 1)
                    .where(MF_LOCKED_BLOCK.ID.eq(lockedBlock.id.value))
                    .and(MF_LOCKED_BLOCK.VERSION.eq(lockedBlock.version))
                    .execute()
            },
            rowExists = {
                dsl.select(MF_LOCKED_BLOCK.ID).from(MF_LOCKED_BLOCK).where(MF_LOCKED_BLOCK.ID.eq(lockedBlock.id.value)).forUpdate().fetchOne() != null
            },
            insert = {
                dsl.insertInto(MF_LOCKED_BLOCK)
                    .set(MF_LOCKED_BLOCK.ID, lockedBlock.id.value)
                    .set(MF_LOCKED_BLOCK.WORLD_ID, lockedBlock.block.worldId.toString())
                    .set(MF_LOCKED_BLOCK.X, lockedBlock.block.x)
                    .set(MF_LOCKED_BLOCK.Y, lockedBlock.block.y)
                    .set(MF_LOCKED_BLOCK.Z, lockedBlock.block.z)
                    .set(MF_LOCKED_BLOCK.CHUNK_X, lockedBlock.chunkX)
                    .set(MF_LOCKED_BLOCK.CHUNK_Z, lockedBlock.chunkZ)
                    .set(MF_LOCKED_BLOCK.PLAYER_ID, lockedBlock.playerId.value)
                    .set(MF_LOCKED_BLOCK.VERSION, 1)
                    .execute()
            }
        )
        if (!written) return null
        return dsl.selectFrom(MF_LOCKED_BLOCK)
            .where(MF_LOCKED_BLOCK.ID.eq(lockedBlock.id.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    override fun delete(block: MfBlockPosition) {
        dsl.deleteFrom(MF_LOCKED_BLOCK)
            .where(MF_LOCKED_BLOCK.WORLD_ID.eq(block.worldId.toString()))
            .and(MF_LOCKED_BLOCK.X.eq(block.x))
            .and(MF_LOCKED_BLOCK.Y.eq(block.y))
            .and(MF_LOCKED_BLOCK.Z.eq(block.z))
            .execute()
    }

    private fun deleteAccessors(dsl: DSLContext, lockedBlockId: MfLockedBlockId) {
        dsl.deleteFrom(MF_LOCKED_BLOCK_ACCESSOR)
            .where(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID.eq(lockedBlockId.value))
            .execute()
    }

    private fun upsertAccessor(dsl: DSLContext, lockedBlockId: MfLockedBlockId, playerId: MfPlayerId): MfPlayerId {
        dsl.insertInto(MF_LOCKED_BLOCK_ACCESSOR)
            .set(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID, lockedBlockId.value)
            .set(MF_LOCKED_BLOCK_ACCESSOR.PLAYER_ID, playerId.value)
            .onConflict(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID, MF_LOCKED_BLOCK_ACCESSOR.PLAYER_ID).doNothing()
            .execute()
        return dsl.selectFrom(MF_LOCKED_BLOCK_ACCESSOR)
            .where(MF_LOCKED_BLOCK_ACCESSOR.LOCKED_BLOCK_ID.eq(lockedBlockId.value))
            .and(MF_LOCKED_BLOCK_ACCESSOR.PLAYER_ID.eq(playerId.value))
            .fetchOne()
            .let(::requireNotNull)
            .playerId
            .let(::MfPlayerId)
    }

    private fun MfLockedBlockRecord.toDomain(accessors: List<MfPlayerId> = emptyList()) = DomainMfLockedBlock(
        id = id.let(::MfLockedBlockId),
        version = version,
        block = MfBlockPosition(
            worldId = UUID.fromString(worldId),
            x = x,
            y = y,
            z = z
        ),
        chunkX = chunkX,
        chunkZ = chunkZ,
        playerId = playerId.let(::MfPlayerId),
        accessors = accessors
    )
}
