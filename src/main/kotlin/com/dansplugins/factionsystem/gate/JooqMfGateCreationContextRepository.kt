package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_GATE_CREATION_CONTEXT
import com.dansplugins.factionsystem.jooq.tables.records.MfGateCreationContextRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext
import java.util.*

class JooqMfGateCreationContextRepository(private val dsl: DSLContext) : MfGateCreationContextRepository {
    override fun getContext(playerId: MfPlayerId): MfGateCreationContext? {
        return dsl.selectFrom(MF_GATE_CREATION_CONTEXT)
            .where(MF_GATE_CREATION_CONTEXT.PLAYER_ID.eq(playerId.value))
            .fetchOne()
            ?.toDomain()
    }

    override fun upsert(context: MfGateCreationContext): MfGateCreationContext {
        val rowCount = dsl.insertInto(MF_GATE_CREATION_CONTEXT)
            .set(MF_GATE_CREATION_CONTEXT.PLAYER_ID, context.playerId.value)
            .set(MF_GATE_CREATION_CONTEXT.VERSION, 1)
            .set(MF_GATE_CREATION_CONTEXT.WORLD_ID, context.position1?.worldId?.toString())
            .set(MF_GATE_CREATION_CONTEXT.X_1, context.position1?.x)
            .set(MF_GATE_CREATION_CONTEXT.Y_1, context.position1?.y)
            .set(MF_GATE_CREATION_CONTEXT.Z_1, context.position1?.z)
            .set(MF_GATE_CREATION_CONTEXT.X_2, context.position2?.x)
            .set(MF_GATE_CREATION_CONTEXT.Y_2, context.position2?.y)
            .set(MF_GATE_CREATION_CONTEXT.Z_2, context.position2?.z)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_X, context.trigger?.x)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_Y, context.trigger?.y)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_Z, context.trigger?.z)
            .onConflict(MF_GATE_CREATION_CONTEXT.PLAYER_ID).doUpdate()
            .set(MF_GATE_CREATION_CONTEXT.WORLD_ID, context.position1?.worldId?.toString())
            .set(MF_GATE_CREATION_CONTEXT.X_1, context.position1?.x)
            .set(MF_GATE_CREATION_CONTEXT.Y_1, context.position1?.y)
            .set(MF_GATE_CREATION_CONTEXT.Z_1, context.position1?.z)
            .set(MF_GATE_CREATION_CONTEXT.X_2, context.position2?.x)
            .set(MF_GATE_CREATION_CONTEXT.Y_2, context.position2?.y)
            .set(MF_GATE_CREATION_CONTEXT.Z_2, context.position2?.z)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_X, context.trigger?.x)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_Y, context.trigger?.y)
            .set(MF_GATE_CREATION_CONTEXT.TRIGGER_Z, context.trigger?.z)
            .set(MF_GATE_CREATION_CONTEXT.VERSION, context.version + 1)
            .where(MF_GATE_CREATION_CONTEXT.PLAYER_ID.eq(context.playerId.value))
            .and(MF_GATE_CREATION_CONTEXT.VERSION.eq(context.version))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${context.version}")
        return getContext(context.playerId).let(::requireNotNull)
    }

    override fun delete(playerId: MfPlayerId) {
        dsl.deleteFrom(MF_GATE_CREATION_CONTEXT)
            .where(MF_GATE_CREATION_CONTEXT.PLAYER_ID.eq(playerId.value))
            .execute()
    }

    private fun MfGateCreationContextRecord.toDomain() = MfGateCreationContext(
        playerId.let(::MfPlayerId),
        version,
        if (worldId == null || x_1 == null || y_1 == null || z_1 == null) {
            null
        } else {
            MfBlockPosition(worldId.let(UUID::fromString), x_1, y_1, z_1)
        },
        if (worldId == null || x_2 == null || y_2 == null || z_2 == null) {
            null
        } else {
            MfBlockPosition(worldId.let(UUID::fromString), x_2, y_2, z_2)
        },
        if (worldId == null || triggerX == null || triggerY == null || triggerZ == null) {
            null
        } else {
            MfBlockPosition(worldId.let(UUID::fromString), triggerX, triggerY, triggerZ)
        }
    )
}
