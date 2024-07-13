package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.Assert.assertEquals
import org.junit.Test

class MfFactionApplicationTest {

    @Test
    fun testMfFactionApplication_Initialization() {
        // prepare
        val mfPlayerId = MfPlayerId("test")
        val mfFactionId = MfFactionId.generate()

        // execute
        val mfFactionApplication = MfFactionApplication(mfPlayerId, mfFactionId)

        // verify
        assertEquals(mfPlayerId, mfFactionApplication.applicantId)
        assertEquals(mfFactionId, mfFactionApplication.factionId)
    }
}
