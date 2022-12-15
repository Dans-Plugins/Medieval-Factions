package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_GATE
import com.dansplugins.factionsystem.jooq.tables.records.MfGateRecord
import org.bukkit.Material
import org.jooq.DSLContext
import java.util.*

class JooqMfGateRepository(
    private val plugin: MedievalFactions,
    private val dsl: DSLContext
) : MfGateRepository {
    override fun getGate(id: MfGateId): MfGate? {
        return dsl.selectFrom(MF_GATE)
            .where(MF_GATE.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()
    }

    override fun getGates(): List<MfGate> {
        return dsl.selectFrom(MF_GATE)
            .fetch()
            .map { it.toDomain() }
    }

    override fun upsert(gate: MfGate): MfGate {
        val rowCount = dsl.insertInto(MF_GATE)
            .set(MF_GATE.ID, gate.id.value)
            .set(MF_GATE.VERSION, 1)
            .set(MF_GATE.FACTION_ID, gate.factionId.value)
            .set(MF_GATE.WORLD_ID, gate.area.minPosition.worldId.toString())
            .set(MF_GATE.MIN_X, gate.area.minPosition.x)
            .set(MF_GATE.MIN_Y, gate.area.minPosition.y)
            .set(MF_GATE.MIN_Z, gate.area.minPosition.z)
            .set(MF_GATE.MAX_X, gate.area.maxPosition.x)
            .set(MF_GATE.MAX_Y, gate.area.maxPosition.y)
            .set(MF_GATE.MAX_Z, gate.area.maxPosition.z)
            .set(MF_GATE.TRIGGER_X, gate.trigger.x)
            .set(MF_GATE.TRIGGER_Y, gate.trigger.y)
            .set(MF_GATE.TRIGGER_Z, gate.trigger.z)
            .set(MF_GATE.MATERIAL, gate.material.name)
            .set(MF_GATE.STATUS, gate.status.name)
            .onConflict(MF_GATE.ID).doUpdate()
            .set(MF_GATE.FACTION_ID, gate.factionId.value)
            .set(MF_GATE.WORLD_ID, gate.area.minPosition.worldId.toString())
            .set(MF_GATE.MIN_X, gate.area.minPosition.x)
            .set(MF_GATE.MIN_Y, gate.area.minPosition.y)
            .set(MF_GATE.MIN_Z, gate.area.minPosition.z)
            .set(MF_GATE.MAX_X, gate.area.maxPosition.x)
            .set(MF_GATE.MAX_Y, gate.area.maxPosition.y)
            .set(MF_GATE.MAX_Z, gate.area.maxPosition.z)
            .set(MF_GATE.TRIGGER_X, gate.trigger.x)
            .set(MF_GATE.TRIGGER_Y, gate.trigger.y)
            .set(MF_GATE.TRIGGER_Z, gate.trigger.z)
            .set(MF_GATE.MATERIAL, gate.material.name)
            .set(MF_GATE.STATUS, gate.status.name)
            .set(MF_GATE.VERSION, gate.version + 1)
            .where(MF_GATE.ID.eq(gate.id.value))
            .and(MF_GATE.VERSION.eq(gate.version))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${gate.version}")
        return getGate(gate.id).let(::requireNotNull)
    }

    override fun delete(gateId: MfGateId) {
        dsl.deleteFrom(MF_GATE)
            .where(MF_GATE.ID.eq(gateId.value))
            .execute()
    }

    override fun deleteAll(factionId: MfFactionId) {
        dsl.deleteFrom(MF_GATE)
            .where(MF_GATE.FACTION_ID.eq(factionId.value))
            .execute()
    }

    private fun MfGateRecord.toDomain() = MfGate(
        plugin,
        id.let(::MfGateId),
        version,
        factionId.let(::MfFactionId),
        MfCuboidArea(
            MfBlockPosition(worldId.let(UUID::fromString), minX, minY, minZ),
            MfBlockPosition(worldId.let(UUID::fromString), maxX, maxY, maxZ)
        ),
        MfBlockPosition(worldId.let(UUID::fromString), triggerX, triggerY, triggerZ),
        material.let(Material::getMaterial).let(::requireNotNull),
        status.let(MfGateStatus::valueOf)
    )
}
