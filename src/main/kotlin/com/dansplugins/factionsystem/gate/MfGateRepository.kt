package com.dansplugins.factionsystem.gate

interface MfGateRepository {

    fun getGate(id: MfGateId): MfGate?
    fun getGates(): List<MfGate>
    fun upsert(gate: MfGate): MfGate
    fun delete(gateId: MfGateId)

}