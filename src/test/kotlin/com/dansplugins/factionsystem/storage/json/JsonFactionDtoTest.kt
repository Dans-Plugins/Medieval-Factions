package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/**
 * Tests for the recovery branches in [JsonFactionDto.toDomain].
 *
 * A hand-edited or partially written factions file can name a role that no longer exists. Neither branch
 * is reachable through the repository's own write path, so they are exercised against the DTO directly.
 */
class JsonFactionDtoTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var logger: Logger

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)
    }

    private fun roleDto(name: String) = JsonFactionRoleDto(UUID.randomUUID().toString(), name)

    private fun factionDto(
        members: List<JsonFactionMemberDto>,
        roles: JsonFactionRolesDto
    ) = JsonFactionDto(
        id = UUID.randomUUID().toString(),
        version = 1,
        name = "TestFaction",
        members = members,
        roles = roles
    )

    @Test
    fun `falls back to the default role when a member names a role that no longer exists`() {
        val owner = roleDto("Owner")
        val default = roleDto("Member")
        val playerId = UUID.randomUUID().toString()
        val missingRoleId = UUID.randomUUID().toString()

        val faction = factionDto(
            members = listOf(JsonFactionMemberDto(playerId, missingRoleId)),
            roles = JsonFactionRolesDto(defaultRoleId = default.id, roles = listOf(owner, default))
        ).toDomain(plugin)

        assertEquals(1, faction.members.size, "the member should be kept, not dropped")
        assertEquals(default.id, faction.members.single().role.id.value)
        assertEquals("Member", faction.members.single().role.name)

        val warning = ArgumentCaptor.forClass(String::class.java)
        verify(logger).warning(warning.capture())
        assertTrue(
            warning.value.contains(missingRoleId) && warning.value.contains(playerId),
            "the warning should name the member and the unknown role, but was: ${warning.value}"
        )
    }

    @Test
    fun `drops a member whose role is unknown when there is no usable default role`() {
        val owner = roleDto("Owner")
        val goodPlayerId = UUID.randomUUID().toString()
        val droppedPlayerId = UUID.randomUUID().toString()
        val missingRoleId = UUID.randomUUID().toString()

        val faction = factionDto(
            members = listOf(
                JsonFactionMemberDto(goodPlayerId, owner.id),
                JsonFactionMemberDto(droppedPlayerId, missingRoleId)
            ),
            // The default role id names a role that is not in the list either.
            roles = JsonFactionRolesDto(defaultRoleId = UUID.randomUUID().toString(), roles = listOf(owner))
        ).toDomain(plugin)

        // Only the unresolvable member is lost; the rest of the faction still loads.
        assertEquals(1, faction.members.size)
        assertEquals(goodPlayerId, faction.members.single().playerId.value)
        assertEquals(owner.id, faction.members.single().role.id.value)

        val severe = ArgumentCaptor.forClass(String::class.java)
        verify(logger).severe(severe.capture())
        assertTrue(
            severe.value.contains(droppedPlayerId) && severe.value.contains(missingRoleId),
            "the error should name the dropped member and the unknown role, but was: ${severe.value}"
        )
    }

    @Test
    fun `keeps every member when all roles resolve`() {
        val owner = roleDto("Owner")
        val member = roleDto("Member")
        val ownerPlayerId = UUID.randomUUID().toString()
        val memberPlayerId = UUID.randomUUID().toString()

        val faction = factionDto(
            members = listOf(
                JsonFactionMemberDto(ownerPlayerId, owner.id),
                JsonFactionMemberDto(memberPlayerId, member.id)
            ),
            roles = JsonFactionRolesDto(defaultRoleId = member.id, roles = listOf(owner, member))
        ).toDomain(plugin)

        assertEquals(2, faction.members.size)
        assertEquals(owner.id, faction.members.first { it.playerId.value == ownerPlayerId }.role.id.value)
        assertEquals(member.id, faction.members.first { it.playerId.value == memberPlayerId }.role.id.value)
        verify(logger, never()).warning(anyString())
        verify(logger, never()).severe(anyString())
    }
}
