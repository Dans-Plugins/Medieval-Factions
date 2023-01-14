package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.faction.MfFactionId

interface MfGateRepository {

    fun getGate(id: MfGateId): MfGate?
    fun getGates(): List<MfGate>
    fun upsert(gate: MfGate): MfGate
    fun delete(gateId: MfGateId)
    fun deleteAll(factionId: MfFactionId)
}
