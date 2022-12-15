package com.dansplugins.factionsystem.duel

interface MfDuelRepository {
    fun getDuels(): List<MfDuel>
    fun upsert(duel: MfDuel): MfDuel
    fun delete(id: MfDuelId)
}
