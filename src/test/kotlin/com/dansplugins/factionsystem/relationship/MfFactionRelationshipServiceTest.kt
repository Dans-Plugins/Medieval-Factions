package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.event.relationship.RelationshipCreateEvent
import com.dansplugins.factionsystem.event.relationship.RelationshipDeleteEvent
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.bukkit.Server
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Logger

/**
 * Characterizes MfFactionRelationshipService: the cache it serves reads from, the events save and delete fire,
 * and the rules that make an alliance or a vassalage count only when both factions hold the matching relationship.
 */
class MfFactionRelationshipServiceTest {

    private val testUtils = TestUtils()
    private lateinit var plugin: MedievalFactions
    private lateinit var pluginManager: PluginManager
    private lateinit var repository: InMemoryRelationshipRepository

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val server = mock(Server::class.java)
        pluginManager = mock(PluginManager::class.java)
        `when`(server.pluginManager).thenReturn(pluginManager)
        `when`(server.isPrimaryThread).thenReturn(true)
        `when`(plugin.server).thenReturn(server)
        repository = InMemoryRelationshipRepository()
    }

    private fun service() = MfFactionRelationshipService(plugin, repository)

    private fun relationship(factionId: MfFactionId, targetId: MfFactionId, type: MfFactionRelationshipType) =
        MfFactionRelationship(factionId = factionId, targetId = targetId, type = type)

    private fun MfFactionRelationshipService.saved(relationship: MfFactionRelationship): MfFactionRelationship {
        val result = save(relationship)
        assertTrue(result is Success, "expected a success, was $result")
        return (result as Success).value
    }

    private fun cancelEvents() {
        doAnswer { invocation ->
            (invocation.arguments[0] as Cancellable).isCancelled = true
            null
        }.`when`(pluginManager).callEvent(any(Event::class.java))
    }

    // Loading and lookups

    @Test
    fun `relationships already in storage are served after construction`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val stored = repository.upsert(relationship(a, b, ALLY))

        val service = service()

        assertEquals(stored, service.getRelationship(stored.id))
        assertEquals(listOf(stored), service.getRelationships(a))
    }

    @Test
    fun `lookups filter by faction, target and type`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val c = testUtils.createFactionId()
        val service = service()
        val allyB = service.saved(relationship(a, b, ALLY))
        val warC = service.saved(relationship(a, c, AT_WAR))
        service.saved(relationship(b, a, ALLY))

        assertEquals(setOf(allyB, warC), service.getRelationships(a).toSet())
        assertEquals(listOf(allyB), service.getRelationships(a, b))
        assertEquals(listOf(warC), service.getRelationships(a, AT_WAR))
        assertEquals(emptyList<MfFactionRelationship>(), service.getRelationships(c))
    }

    @Test
    fun `an unknown relationship id returns null`() {
        assertNull(service().getRelationship(MfFactionRelationshipId.generate()))
    }

    // Save

    @Test
    fun `saving a new relationship fires a create event, stores it and caches it`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val service = service()
        val toSave = relationship(a, b, ALLY)

        val saved = service.saved(toSave)

        val captor = ArgumentCaptor.forClass(Event::class.java)
        verify(pluginManager).callEvent(captor.capture())
        val event = captor.value as RelationshipCreateEvent
        assertEquals(toSave.id, event.relationshipId)
        assertEquals(toSave, event.relationship)
        assertFalse(event.isAsynchronous, "a save on the main thread fires a synchronous event")
        assertEquals(saved, repository.getFactionRelationship(toSave.id))
        assertEquals(saved, service.getRelationship(toSave.id))
    }

    @Test
    fun `saving off the main thread fires an asynchronous create event`() {
        `when`(plugin.server.isPrimaryThread).thenReturn(false)
        val service = service()

        service.saved(relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY))

        val captor = ArgumentCaptor.forClass(Event::class.java)
        verify(pluginManager).callEvent(captor.capture())
        assertTrue(captor.value.isAsynchronous)
    }

    @Test
    fun `re-saving a relationship already in the cache fires no create event`() {
        val service = service()
        val saved = service.saved(relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY))

        service.saved(saved.copy(type = AT_WAR))

        // Only the first save fired an event.
        verify(pluginManager, times(1)).callEvent(any(Event::class.java))
        assertEquals(AT_WAR, service.getRelationship(saved.id)?.type)
    }

    @Test
    fun `a cancelled create event fails the save and stores nothing`() {
        cancelEvents()
        val service = service()
        val toSave = relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY)

        val result = service.save(toSave)

        assertTrue(result is Failure && result.reason.type == GENERAL, "expected a GENERAL failure, was $result")
        assertNull(repository.getFactionRelationship(toSave.id))
        assertNull(service.getRelationship(toSave.id))
    }

    @Test
    fun `an optimistic locking failure in the repository is reported as a conflict`() {
        repository.failUpsertWith = OptimisticLockingFailureException("stale")
        val service = service()
        val toSave = relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY)

        val result = service.save(toSave)

        assertTrue(result is Failure && result.reason.type == CONFLICT, "expected a CONFLICT, was $result")
        assertNull(service.getRelationship(toSave.id))
    }

    @Test
    fun `any other repository failure is reported as a general failure`() {
        repository.failUpsertWith = IllegalStateException("boom")
        val service = service()

        val result = service.save(relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY))

        assertTrue(result is Failure && result.reason.type == GENERAL, "expected a GENERAL failure, was $result")
    }

    // Delete

    @Test
    fun `deleting fires a delete event and removes the relationship from storage and cache`() {
        val service = service()
        val saved = service.saved(relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY))

        val result = service.delete(saved.id)

        assertTrue(result is Success, "expected a success, was $result")
        val captor = ArgumentCaptor.forClass(Event::class.java)
        verify(pluginManager, times(2)).callEvent(captor.capture())
        val event = captor.allValues.last() as RelationshipDeleteEvent
        assertEquals(saved.id, event.relationshipId)
        assertNull(repository.getFactionRelationship(saved.id))
        assertNull(service.getRelationship(saved.id))
    }

    @Test
    fun `a cancelled delete event fails the delete and keeps the relationship`() {
        val service = service()
        val saved = service.saved(relationship(testUtils.createFactionId(), testUtils.createFactionId(), ALLY))
        cancelEvents()

        val result = service.delete(saved.id)

        assertTrue(result is Failure && result.reason.type == GENERAL, "expected a GENERAL failure, was $result")
        assertEquals(saved, repository.getFactionRelationship(saved.id))
        assertEquals(saved, service.getRelationship(saved.id))
    }

    // Allies

    @Test
    fun `an alliance counts only when both factions hold it`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(a, b, ALLY))

        assertEquals(emptyList<MfFactionId>(), service.getAllies(a), "a one-sided alliance request is not an alliance")
        assertEquals(emptyList<MfFactionId>(), service.getAllies(b))

        service.saved(relationship(b, a, ALLY))

        assertEquals(listOf(b), service.getAllies(a))
        assertEquals(listOf(a), service.getAllies(b))
    }

    // Vassalage

    @Test
    fun `a vassalage counts only when the vassal holds the matching liege relationship`() {
        val liege = testUtils.createFactionId()
        val vassal = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(liege, vassal, VASSAL))

        assertEquals(emptyList<MfFactionId>(), service.getVassals(liege), "an offer of vassalage is not a vassalage")
        assertNull(service.getLiege(vassal))

        service.saved(relationship(vassal, liege, LIEGE))

        assertEquals(listOf(vassal), service.getVassals(liege))
        assertEquals(liege, service.getLiege(vassal))
    }

    @Test
    fun `a liege relationship without the liege's vassal relationship gives no liege`() {
        val liege = testUtils.createFactionId()
        val vassal = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(vassal, liege, LIEGE))

        assertNull(service.getLiege(vassal))
        assertEquals(emptyList<MfFactionId>(), service.getVassals(liege))
    }

    @Test
    fun `the vassal tree and liege chain follow confirmed vassalages through several levels`() {
        val emperor = testUtils.createFactionId()
        val king = testUtils.createFactionId()
        val duke = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(emperor, king, VASSAL))
        service.saved(relationship(king, emperor, LIEGE))
        service.saved(relationship(king, duke, VASSAL))
        service.saved(relationship(duke, king, LIEGE))

        assertEquals(
            MfVassalNode(emperor, listOf(MfVassalNode(king, listOf(MfVassalNode(duke, emptyList()))))),
            service.getVassalTree(emperor)
        )
        assertEquals(
            MfLiegeNode(duke, MfLiegeNode(king, MfLiegeNode(emperor, null))),
            service.getLiegeChain(duke)
        )
    }

    @Test
    fun `a faction with no liege has a single-node liege chain`() {
        val faction = testUtils.createFactionId()

        assertEquals(MfLiegeNode(faction, null), service().getLiegeChain(faction))
    }

    // War

    @Test
    fun `a one-sided war declaration puts both factions at war with each other`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(a, b, AT_WAR))

        assertEquals(listOf(b), service.getFactionsAtWarWith(a))
        assertEquals(listOf(a), service.getFactionsAtWarWith(b))
    }

    @Test
    fun `wars declared in both directions list each enemy once`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val c = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(a, b, AT_WAR))
        service.saved(relationship(b, a, AT_WAR))
        service.saved(relationship(c, a, AT_WAR))

        assertEquals(setOf(b, c), service.getFactionsAtWarWith(a).toSet())
        assertEquals(2, service.getFactionsAtWarWith(a).size)
    }

    @Test
    fun `other relationship types do not count as war`() {
        val a = testUtils.createFactionId()
        val b = testUtils.createFactionId()
        val service = service()
        service.saved(relationship(a, b, ALLY))
        service.saved(relationship(a, b, VASSAL))

        assertEquals(emptyList<MfFactionId>(), service.getFactionsAtWarWith(a))
    }

    private class InMemoryRelationshipRepository : MfFactionRelationshipRepository {
        val stored = linkedMapOf<MfFactionRelationshipId, MfFactionRelationship>()
        var failUpsertWith: Exception? = null

        override fun getFactionRelationship(relationshipId: MfFactionRelationshipId) = stored[relationshipId]
        override fun getFactionRelationships(factionId: MfFactionId, targetId: MfFactionId) =
            stored.values.filter { it.factionId == factionId && it.targetId == targetId }
        override fun getFactionRelationships(factionId: MfFactionId, type: MfFactionRelationshipType) =
            stored.values.filter { it.factionId == factionId && it.type == type }
        override fun getFactionRelationships(factionId: MfFactionId) = stored.values.filter { it.factionId == factionId }
        override fun getFactionRelationships() = stored.values.toList()
        override fun upsert(relationship: MfFactionRelationship): MfFactionRelationship {
            failUpsertWith?.let { throw it }
            stored[relationship.id] = relationship
            return relationship
        }
        override fun delete(relationshipId: MfFactionRelationshipId) {
            stored.remove(relationshipId)
        }
    }
}
