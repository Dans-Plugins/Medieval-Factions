package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import org.mockito.Mockito.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MfFactionRolesMoveTest {

    private lateinit var owner: MfFactionRole
    private lateinit var officer: MfFactionRole
    private lateinit var member: MfFactionRole
    private lateinit var duke: MfFactionRole
    private lateinit var roles: MfFactionRoles

    @BeforeTest
    fun setUp() {
        val plugin = mock(MedievalFactions::class.java)
        owner = MfFactionRole(plugin, id = MfFactionRoleId("owner"), name = "Owner", permissionsByName = mapOf("CLAIM" to true))
        officer = MfFactionRole(plugin, id = MfFactionRoleId("officer"), name = "Officer")
        member = MfFactionRole(plugin, id = MfFactionRoleId("member"), name = "Member")
        duke = MfFactionRole(plugin, id = MfFactionRoleId("duke"), name = "Duke")
        roles = MfFactionRoles(member.id, listOf(owner, officer, member, duke))
    }

    private fun MfFactionRoles?.names() = this?.map { it.name }

    @Test
    fun movingUpSwapsTheRoleWithTheOneAboveIt() {
        assertEquals(listOf("Owner", "Officer", "Duke", "Member"), roles.move(duke.id, -1).names())
    }

    @Test
    fun movingDownSwapsTheRoleWithTheOneBelowIt() {
        assertEquals(listOf("Officer", "Owner", "Member", "Duke"), roles.move(owner.id, 1).names())
    }

    @Test
    fun repeatedMovesCarryARoleAboveOlderRoles() {
        val moved = roles.move(duke.id, -1)!!.move(duke.id, -1)
        assertEquals(listOf("Owner", "Duke", "Officer", "Member"), moved.names())
    }

    @Test
    fun theTopRoleCannotMoveUp() {
        assertNull(roles.move(owner.id, -1))
    }

    @Test
    fun theBottomRoleCannotMoveDown() {
        assertNull(roles.move(duke.id, 1))
    }

    @Test
    fun anUnknownRoleCannotMove() {
        assertNull(roles.move(MfFactionRoleId("missing"), -1))
    }

    @Test
    fun movingKeepsTheDefaultRoleAndEveryRoleUnchanged() {
        val moved = roles.move(duke.id, -1)!!
        assertEquals(member.id, moved.defaultRoleId)
        assertEquals(roles.roles.toSet(), moved.roles.toSet())
        assertEquals(mapOf<String, Boolean?>("CLAIM" to true), moved.getRole(owner.id)!!.permissionsByName)
    }

    @Test
    fun moveRoleKeepsItsJvmNameForJavaCallers() {
        MfFactionRoles::class.java.getMethod("moveRole", String::class.java, Int::class.javaPrimitiveType)
    }
}
