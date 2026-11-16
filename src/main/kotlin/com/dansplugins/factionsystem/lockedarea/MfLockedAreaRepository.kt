package com.dansplugins.factionsystem.lockedarea

interface MfLockedAreaRepository {
    fun getLockedAreas(): List<MfLockedArea>
    fun upsert(lockedArea: MfLockedArea): MfLockedArea
    fun delete(id: MfLockedAreaId)
}
