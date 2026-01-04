package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.player.MfPlayerId
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class MfFactionMemberTest {

    @Test
    fun testMemberInitialization() {
        // prepare
        val playerId = MfPlayerId("test-player-id")
        val mockRole = mock(MfFactionRole::class.java)

        // execute
        val member = MfFactionMember(playerId, mockRole)

        // verify
        assertEquals(playerId, member.playerId)
        assertEquals(mockRole, member.role)
    }

    @Test
    fun testMemberWithDifferentRoles() {
        // prepare
        val playerId = MfPlayerId("test-player-id")
        val role1 = mock(MfFactionRole::class.java)
        val role2 = mock(MfFactionRole::class.java)

        // execute
        val member1 = MfFactionMember(playerId, role1)
        val member2 = MfFactionMember(playerId, role2)

        // verify
        assertEquals(role1, member1.role)
        assertEquals(role2, member2.role)
        assertEquals(member1.playerId, member2.playerId)
    }

    @Test
    fun testMultipleMembersWithDifferentIds() {
        // prepare
        val playerId1 = MfPlayerId("player-1")
        val playerId2 = MfPlayerId("player-2")
        val mockRole = mock(MfFactionRole::class.java)

        // execute
        val member1 = MfFactionMember(playerId1, mockRole)
        val member2 = MfFactionMember(playerId2, mockRole)

        // verify
        assertEquals(playerId1, member1.playerId)
        assertEquals(playerId2, member2.playerId)
        assertEquals(member1.role, member2.role)
    }
}
