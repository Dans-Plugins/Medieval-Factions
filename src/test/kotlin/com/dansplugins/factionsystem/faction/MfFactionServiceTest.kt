package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.event.faction.FactionCreateEvent
import com.dansplugins.factionsystem.event.faction.FactionDescriptionChangeEvent
import com.dansplugins.factionsystem.event.faction.FactionDisbandEvent
import com.dansplugins.factionsystem.event.faction.FactionJoinEvent
import com.dansplugins.factionsystem.event.faction.FactionLeaveEvent
import com.dansplugins.factionsystem.event.faction.FactionPrefixChangeEvent
import com.dansplugins.factionsystem.event.faction.FactionRenameEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.field.MfFactionField
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.map.MapService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Characterises MfFactionService: what the in-memory cache holds, which events a save raises, what a cancelled event
 * or a failed write leaves behind, and what is removed with a faction.
 */
class MfFactionServiceTest {

    /** Stands in for storage: a write returns the next version, as both real repositories do. */
    private inner class FakeRepository(vararg stored: MfFaction) : MfFactionRepository {
        val factions = stored.associateBy(MfFaction::id).toMutableMap()
        val upserted = mutableListOf<MfFaction>()
        val deleted = mutableListOf<MfFactionId>()
        var failure: Exception? = null

        override fun getFaction(id: MfFactionId) = factions[id]
        override fun getFaction(name: String) = factions.values.firstOrNull { it.name == name }
        override fun getFaction(playerId: MfPlayerId) = factions.values.firstOrNull { faction ->
            faction.members.any { it.playerId == playerId }
        }
        override fun getFactions() = factions.values.toList()

        override fun upsert(faction: MfFaction): MfFaction {
            failure?.let { throw it }
            upserted += faction
            val saved = faction.copy(version = faction.version + 1)
            factions[saved.id] = saved
            return saved
        }

        override fun delete(factionId: MfFactionId) {
            failure?.let { throw it }
            deleted += factionId
            factions.remove(factionId)
        }
    }

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var server: Server
    private lateinit var scheduler: BukkitScheduler
    private lateinit var services: Services
    private lateinit var lockService: MfLockService
    private lateinit var claimService: MfClaimService
    private lateinit var gateService: MfGateService
    private lateinit var flags: MfFlags
    private lateinit var memberRole: MfFactionRole

    private val raisedEvents = mutableListOf<Event>()
    private var cancelWhen: (Event) -> Boolean = { false }

    private val factionId = MfFactionId("faction")
    private val owner = MfPlayerId("owner")
    private val member = MfPlayerId("member")

    @BeforeTest
    fun setUp() {
        raisedEvents.clear()
        cancelWhen = { false }

        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)

        config = mock(FileConfiguration::class.java)
        `when`(config.getBoolean("factions.allowNeutrality")).thenReturn(true)
        `when`(plugin.config).thenReturn(config)

        val permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)

        server = mock(Server::class.java)
        scheduler = mock(BukkitScheduler::class.java)
        val pluginManager = mock(PluginManager::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(server.pluginManager).thenReturn(pluginManager)
        doAnswer { invocation ->
            val event = invocation.getArgument<Event>(0)
            raisedEvents += event
            if (cancelWhen(event)) (event as Cancellable).isCancelled = true
            null
        }.`when`(pluginManager).callEvent(any())

        lockService = mock(MfLockService::class.java)
        `when`(lockService.getLockedBlocks(owner)).thenReturn(emptyList())
        `when`(lockService.getLockedBlocks(member)).thenReturn(emptyList())
        claimService = mock(MfClaimService::class.java)
        gateService = mock(MfGateService::class.java)
        `when`(claimService.deleteAllClaims(factionId)).thenReturn(Success(Unit))
        `when`(gateService.deleteAllGates(factionId)).thenReturn(Success(Unit))

        services = mock(Services::class.java)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.gateService).thenReturn(gateService)
        `when`(plugin.services).thenReturn(services)

        memberRole = MfFactionRole(plugin, id = MfFactionRoleId("role"), name = "Member")
    }

    private fun faction(
        id: MfFactionId = factionId,
        name: String = "Avalon",
        members: List<MfPlayerId> = listOf(owner, member),
        flagValues: Map<String, Any?> = emptyMap(),
        applications: List<MfFactionApplication> = emptyList()
    ) = MfFaction(
        plugin,
        id = id,
        name = name,
        description = "A faction",
        prefix = "AV",
        members = members.map { MfFactionMember(it, memberRole) },
        flags = MfFlagValues(plugin, flagValues),
        roles = MfFactionRoles(memberRole.id, listOf(memberRole)),
        defaultPermissionsByName = emptyMap(),
        applications = applications
    )

    private fun lockedBy(playerId: MfPlayerId, x: Int): MfLockedBlock {
        val position = MfBlockPosition(UUID.randomUUID(), x, 64, 0)
        `when`(lockService.delete(position)).thenReturn(Success(Unit))
        return MfLockedBlock(block = position, chunkX = 0, chunkZ = 0, playerId = playerId, accessors = emptyList())
    }

    private fun <T> failureOf(result: Result4k<T, ServiceFailure>): ServiceFailure = when (result) {
        is Failure -> result.reason
        is Success -> fail("expected a failure but was $result")
    }

    private fun <T> valueOf(result: Result4k<T, ServiceFailure>): T = when (result) {
        is Success -> result.value
        is Failure -> fail("expected a success but was ${result.reason}")
    }

    // Loading

    @Test
    fun loadsStoredFactionsIntoTheCache() {
        val stored = faction()
        val service = MfFactionService(plugin, FakeRepository(stored))

        assertEquals(listOf(stored.id), service.factions.map(MfFaction::id))
        assertSame(stored, service.getFaction(stored.id))
    }

    @Test
    fun neutralFactionsAreLeftAloneWhileNeutralityIsAllowed() {
        val repository = FakeRepository(faction(flagValues = mapOf("neutral" to true)))

        MfFactionService(plugin, repository)

        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun onlyNeutralFactionsAreRewrittenWhenNeutralityIsNotAllowed() {
        `when`(config.getBoolean("factions.allowNeutrality")).thenReturn(false)
        val neutral = faction(flagValues = mapOf("neutral" to true))
        val other = faction(id = MfFactionId("other"), name = "Other", members = emptyList())
        val repository = FakeRepository(neutral, other)

        val service = MfFactionService(plugin, repository)

        assertEquals(listOf(neutral.id), repository.upserted.map(MfFaction::id))
        val updated = assertNotNull(service.getFaction(neutral.id))
        assertFalse(updated.flags[flags.isNeutral])
        assertEquals(neutral.version + 1, updated.version)
    }

    // Startup builds this service before plugin.services is assigned, so clearing the flag must not read it (#2147).
    @Test
    fun startupClearsEveryNeutralFactionBeforeServicesAreAssigned() {
        `when`(config.getBoolean("factions.allowNeutrality")).thenReturn(false)
        `when`(plugin.services).thenThrow(UninitializedPropertyAccessException("services"))
        val first = faction(flagValues = mapOf("neutral" to true))
        val second = faction(id = MfFactionId("second"), name = "Second", members = emptyList(), flagValues = mapOf("neutral" to true))
        val repository = FakeRepository(first, second)

        val service = MfFactionService(plugin, repository)

        assertEquals(setOf(first.id, second.id), repository.upserted.map(MfFaction::id).toSet())
        assertFalse(assertNotNull(service.getFaction(first.id)).flags[flags.isNeutral])
        assertFalse(assertNotNull(service.getFaction(second.id)).flags[flags.isNeutral])
        assertEquals(first.version + 1, service.getFaction(first.id)?.version)
    }

    @Test
    fun aRefusedNeutralityWriteStopsStartup() {
        `when`(config.getBoolean("factions.allowNeutrality")).thenReturn(false)
        val repository = FakeRepository(faction(flagValues = mapOf("neutral" to true)))
        repository.failure = OptimisticLockingFailureException("stale")

        assertFailsWith<OptimisticLockingFailureException> { MfFactionService(plugin, repository) }
    }

    // Lookups

    @Test
    fun aNameLookupIsCaseSensitive() {
        val service = MfFactionService(plugin, FakeRepository(faction()))

        assertNotNull(service.getFaction("Avalon"))
        assertNull(service.getFaction("avalon"))
    }

    @Test
    fun aNameSharedByTwoFactionsFindsNeither() {
        val service = MfFactionService(
            plugin,
            FakeRepository(faction(), faction(id = MfFactionId("twin"), members = emptyList()))
        )

        assertNull(service.getFaction("Avalon"))
    }

    @Test
    fun aPlayerLookupFindsTheFactionTheyBelongTo() {
        val service = MfFactionService(plugin, FakeRepository(faction()))

        assertEquals(factionId, service.getFaction(member)?.id)
        assertNull(service.getFaction(MfPlayerId("stranger")))
    }

    @Test
    fun aPlayerInTwoFactionsFindsNeither() {
        val service = MfFactionService(
            plugin,
            FakeRepository(faction(), faction(id = MfFactionId("twin"), name = "Twin", members = listOf(member)))
        )

        assertNull(service.getFaction(member))
    }

    // Saving a new faction

    @Test
    fun savingANewFactionRaisesCreateAndCachesTheStoredVersion() {
        val repository = FakeRepository()
        val service = MfFactionService(plugin, repository)

        val saved = valueOf(service.save(faction()))

        assertEquals(1, saved.version)
        assertIs<FactionCreateEvent>(raisedEvents.single())
        assertEquals(1, service.getFaction(factionId)?.version)
    }

    @Test
    fun aCreateListenerCanReplaceTheFactionThatIsStored() {
        val repository = FakeRepository()
        val service = MfFactionService(plugin, repository)
        val replacement = faction(name = "Replaced")
        cancelWhen = { event ->
            (event as? FactionCreateEvent)?.faction = replacement
            false
        }

        val saved = valueOf(service.save(faction()))

        assertEquals("Replaced", saved.name)
        assertEquals("Replaced", repository.upserted.single().name)
    }

    @Test
    fun aCancelledCreateStoresNothing() {
        val repository = FakeRepository()
        val service = MfFactionService(plugin, repository)
        cancelWhen = { it is FactionCreateEvent }

        val failure = failureOf(service.save(faction()))

        assertEquals(GENERAL, failure.type)
        assertIs<EventCancelledException>(failure.cause)
        assertTrue(repository.upserted.isEmpty())
        assertNull(service.getFaction(factionId))
    }

    @Test
    fun aNewFactionsEventIsAsyncOnlyOffTheMainThread() {
        val service = MfFactionService(plugin, FakeRepository())

        `when`(server.isPrimaryThread).thenReturn(false)
        service.save(faction())
        `when`(server.isPrimaryThread).thenReturn(true)
        service.save(faction(id = MfFactionId("other"), members = emptyList()))

        assertEquals(listOf(true, false), raisedEvents.map(Event::isAsynchronous))
    }

    @Test
    fun createFactionFillsInDefaultsAndSaves() {
        val repository = FakeRepository()
        val service = MfFactionService(plugin, repository)

        val created = valueOf(service.createFaction("Avalon", id = factionId.value))

        assertEquals(factionId, created.id)
        assertEquals("Avalon", created.name)
        assertEquals("", created.description)
        assertTrue(created.members.isEmpty())
        // The default colour is random, so the names are compared rather than the values.
        assertEquals(flags.defaults().valuesByName.keys, created.flags.valuesByName.keys)
        assertEquals(3, created.roles.size)
        assertEquals(1, repository.upserted.size)
    }

    // Saving an existing faction

    @Test
    fun savingAnUnchangedFactionRaisesNoEvent() {
        val stored = faction()
        val repository = FakeRepository(stored)

        valueOf(MfFactionService(plugin, repository).save(stored))

        assertTrue(raisedEvents.isEmpty())
        assertEquals(1, repository.upserted.size)
    }

    @Test
    fun eachChangedFieldRaisesItsOwnEvent() {
        val stored = faction()
        val service = MfFactionService(plugin, FakeRepository(stored))

        valueOf(service.save(stored.copy(name = "Renamed", description = "Changed", prefix = "RN")))

        assertEquals(
            listOf(FactionRenameEvent::class, FactionDescriptionChangeEvent::class, FactionPrefixChangeEvent::class),
            raisedEvents.map { it::class }
        )
    }

    @Test
    fun aCancelledFieldChangeLeavesTheStoredFactionAsItWas() {
        val stored = faction()
        listOf<Pair<(Event) -> Boolean, MfFaction>>(
            { event: Event -> event is FactionRenameEvent } to stored.copy(name = "Renamed"),
            { event: Event -> event is FactionDescriptionChangeEvent } to stored.copy(description = "Changed"),
            { event: Event -> event is FactionPrefixChangeEvent } to stored.copy(prefix = "RN"),
            { event: Event -> event is FactionJoinEvent } to stored.copy(members = stored.members + MfFactionMember(MfPlayerId("new"), memberRole)),
            { event: Event -> event is FactionLeaveEvent } to stored.copy(members = stored.members.take(1))
        ).forEach { (cancel, change) ->
            val repository = FakeRepository(stored)
            val service = MfFactionService(plugin, repository)
            cancelWhen = cancel

            val failure = failureOf(service.save(change))

            assertEquals(GENERAL, failure.type)
            assertIs<EventCancelledException>(failure.cause)
            assertTrue(repository.upserted.isEmpty())
            assertSame(stored, service.getFaction(factionId))
        }
    }

    @Test
    fun aNewMemberRaisesOneJoinForThatPlayer() {
        val stored = faction()
        val joining = MfPlayerId("joining")
        val service = MfFactionService(plugin, FakeRepository(stored))

        valueOf(service.save(stored.copy(members = stored.members + MfFactionMember(joining, memberRole))))

        assertEquals(joining, assertIs<FactionJoinEvent>(raisedEvents.single()).playerId)
    }

    @Test
    fun aLeavingMemberRaisesLeaveAndLosesOnlyTheirOwnLocks() {
        val stored = faction()
        val theirs = lockedBy(member, 1)
        val alsoTheirs = lockedBy(member, 2)
        val ownersLock = lockedBy(owner, 3)
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(theirs, alsoTheirs))
        `when`(lockService.getLockedBlocks(owner)).thenReturn(listOf(ownersLock))
        val service = MfFactionService(plugin, FakeRepository(stored))

        valueOf(service.save(stored.copy(members = stored.members.filter { it.playerId == owner })))

        assertEquals(member, assertIs<FactionLeaveEvent>(raisedEvents.single()).playerId)
        verify(lockService).delete(theirs.block)
        verify(lockService).delete(alsoTheirs.block)
        verify(lockService, never()).delete(ownersLock.block)
    }

    @Test
    fun aCancelledLeaveKeepsTheLeaversLocks() {
        val stored = faction()
        val theirs = lockedBy(member, 1)
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(theirs))
        val service = MfFactionService(plugin, FakeRepository(stored))
        cancelWhen = { it is FactionLeaveEvent }

        failureOf(service.save(stored.copy(members = stored.members.take(1))))

        verify(lockService, never()).delete(theirs.block)
    }

    @Test
    fun aLockThatCannotBeDeletedDoesNotFailTheSave() {
        val stored = faction()
        val stuck = lockedBy(member, 1)
        val nextLock = lockedBy(member, 2)
        val cause = RuntimeException("lock storage")
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(stuck, nextLock))
        `when`(lockService.delete(stuck.block)).thenReturn(Failure(ServiceFailure(GENERAL, "lock", cause)))
        val mapService = mock(MapService::class.java)
        `when`(services.mapService).thenReturn(mapService)
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)

        val saved = valueOf(service.save(stored.copy(members = stored.members.take(1))))

        assertEquals(listOf(owner), saved.members.map(MfFactionMember::playerId))
        assertEquals(saved.version, service.getFaction(factionId)?.version)
        verify(plugin.logger).log(eq(Level.SEVERE), anyString(), eq(cause))
        verify(lockService).delete(nextLock.block)
        capturedMapTask().run()
        verify(mapService).scheduleUpdateClaims(saved)
    }

    @Test
    fun theLeaversLocksAreDeletedOnlyAfterTheWrite() {
        val stored = faction()
        val theirs = lockedBy(member, 1)
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(theirs))
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        var writesBeforeDelete = -1
        doAnswer {
            writesBeforeDelete = repository.upserted.size
            Success(Unit)
        }.`when`(lockService).delete(theirs.block)

        valueOf(service.save(stored.copy(members = stored.members.take(1))))

        assertEquals(1, writesBeforeDelete)
    }

    @Test
    fun aRefusedWriteKeepsTheLeaversLocks() {
        val stored = faction()
        val theirs = lockedBy(member, 1)
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(theirs))
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        repository.failure = OptimisticLockingFailureException("stale")

        val failure = failureOf(service.save(stored.copy(members = stored.members.take(1))))

        assertEquals(CONFLICT, failure.type)
        assertTrue(service.getFaction(factionId)!!.members.any { it.playerId == member })
        verify(lockService, never()).delete(theirs.block)
    }

    @Test
    fun aCancelledSecondLeaveKeepsTheFirstLeaversLocks() {
        val second = MfPlayerId("second")
        `when`(lockService.getLockedBlocks(second)).thenReturn(emptyList())
        val stored = faction(members = listOf(owner, member, second))
        val theirs = lockedBy(member, 1)
        `when`(lockService.getLockedBlocks(member)).thenReturn(listOf(theirs))
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        cancelWhen = { it is FactionLeaveEvent && it.playerId == second }

        failureOf(service.save(stored.copy(members = stored.members.take(1))))

        assertTrue(repository.upserted.isEmpty())
        verify(lockService, never()).delete(theirs.block)
    }

    @Test
    fun anOptimisticLockingFailureIsAConflictAndKeepsTheCachedFaction() {
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        repository.failure = OptimisticLockingFailureException("stale")

        val failure = failureOf(service.save(stored.copy(bonusPower = 5.0)))

        assertEquals(CONFLICT, failure.type)
        assertSame(stored, service.getFaction(factionId))
    }

    @Test
    fun anyOtherWriteFailureIsGeneral() {
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        repository.failure = IllegalStateException("disk")

        assertEquals(GENERAL, failureOf(service.save(stored)).type)
    }

    @Test
    fun aSaveCachesTheStoredVersionForTheNextReader() {
        val stored = faction()
        val service = MfFactionService(plugin, FakeRepository(stored))

        valueOf(service.save(assertNotNull(service.getFaction(factionId)).copy(bonusPower = 5.0)))
        assertEquals(stored.version + 1, service.getFaction(factionId)?.version)

        // Commands read the faction from the cache, so this is the version their next save is built on.
        valueOf(service.save(assertNotNull(service.getFaction(factionId)).copy(description = "Changed")))
        assertEquals(stored.version + 2, service.getFaction(factionId)?.version)
        assertEquals(5.0, service.getFaction(factionId)?.bonusPower)
    }

    // Map updates

    private fun capturedMapTask(): Runnable {
        val task = ArgumentCaptor.forClass(Runnable::class.java)
        verify(scheduler).runTask(eq(plugin), task.capture())
        return task.value
    }

    @Test
    fun aSaveRefreshesTheMapOnTheMainThreadWithTheStoredFaction() {
        val mapService = mock(MapService::class.java)
        `when`(services.mapService).thenReturn(mapService)
        val stored = faction()
        val service = MfFactionService(plugin, FakeRepository(stored))

        val saved = valueOf(service.save(stored))

        verifyNoInteractions(mapService)
        capturedMapTask().run()
        verify(mapService).scheduleUpdateClaims(saved)
    }

    @Test
    fun noMapUpdateIsScheduledWhenTerritoriesRenderOnlyAtStartup() {
        `when`(services.mapService).thenReturn(mock(MapService::class.java))
        `when`(config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")).thenReturn(true)
        val stored = faction()

        valueOf(MfFactionService(plugin, FakeRepository(stored)).save(stored))

        verifyNoInteractions(scheduler)
    }

    @Test
    fun noMapUpdateIsScheduledWithoutAMapService() {
        val stored = faction()

        valueOf(MfFactionService(plugin, FakeRepository(stored)).save(stored))

        verifyNoInteractions(scheduler)
    }

    // Deleting

    @Test
    fun deletingAFactionRemovesItsClaimsGatesAndRecord() {
        val repository = FakeRepository(faction())
        val service = MfFactionService(plugin, repository)

        valueOf(service.delete(factionId))

        assertIs<FactionDisbandEvent>(raisedEvents.single())
        verify(claimService).deleteAllClaims(factionId)
        verify(gateService).deleteAllGates(factionId)
        assertEquals(listOf(factionId), repository.deleted)
        assertNull(service.getFaction(factionId))
    }

    @Test
    fun aCancelledDisbandRemovesNothing() {
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        cancelWhen = { it is FactionDisbandEvent }

        val failure = failureOf(service.delete(factionId))

        assertIs<EventCancelledException>(failure.cause)
        verify(claimService, never()).deleteAllClaims(factionId)
        verify(gateService, never()).deleteAllGates(factionId)
        assertTrue(repository.deleted.isEmpty())
        assertSame(stored, service.getFaction(factionId))
    }

    @Test
    fun aClaimFailureStopsTheDeleteBeforeGatesAndTheRecord() {
        val cause = RuntimeException("claims")
        `when`(claimService.deleteAllClaims(factionId)).thenReturn(Failure(ServiceFailure(GENERAL, "claims", cause)))
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)

        val failure = failureOf(service.delete(factionId))

        assertSame(cause, failure.cause)
        verify(gateService, never()).deleteAllGates(factionId)
        assertTrue(repository.deleted.isEmpty())
        assertSame(stored, service.getFaction(factionId))
    }

    @Test
    fun aGateFailureKeepsTheRecord() {
        val cause = RuntimeException("gates")
        `when`(gateService.deleteAllGates(factionId)).thenReturn(Failure(ServiceFailure(GENERAL, "gates", cause)))
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)

        assertSame(cause, failureOf(service.delete(factionId)).cause)
        assertTrue(repository.deleted.isEmpty())
        assertSame(stored, service.getFaction(factionId))
    }

    @Test
    fun aRefusedDeleteIsAConflictAndKeepsTheCachedFaction() {
        val stored = faction()
        val repository = FakeRepository(stored)
        val service = MfFactionService(plugin, repository)
        repository.failure = OptimisticLockingFailureException("stale")

        assertEquals(CONFLICT, failureOf(service.delete(factionId)).type)
        assertSame(stored, service.getFaction(factionId))
    }

    // Applications and fields

    @Test
    fun cancellingApplicationsRemovesOnlyThatPlayersEverywhere() {
        val applicant = MfPlayer(MfPlayerId("applicant"), name = "Applicant")
        val otherApplicant = MfPlayerId("other-applicant")
        val otherId = MfFactionId("other")
        val first = faction(
            applications = listOf(MfFactionApplication(factionId, applicant.id), MfFactionApplication(factionId, otherApplicant))
        )
        val second = faction(
            id = otherId,
            name = "Other",
            members = emptyList(),
            applications = listOf(MfFactionApplication(otherId, applicant.id))
        )
        val service = MfFactionService(plugin, FakeRepository(first, second))

        service.cancelAllApplicationsForPlayer(applicant)

        assertEquals(listOf(otherApplicant), service.getFaction(factionId)?.applications?.map(MfFactionApplication::applicantId))
        assertEquals(emptyList(), service.getFaction(otherId)?.applications)
    }

    @Test
    fun cancellingApplicationsWritesEveryFactionEvenWithNoApplication() {
        val repository = FakeRepository(
            faction(),
            faction(id = MfFactionId("other"), name = "Other", members = emptyList())
        )
        val service = MfFactionService(plugin, repository)

        service.cancelAllApplicationsForPlayer(MfPlayer(MfPlayerId("applicant"), name = "Applicant"))

        assertEquals(2, repository.upserted.size)
    }

    @Test
    fun aFailedApplicationWriteIsRethrown() {
        val repository = FakeRepository(faction())
        val service = MfFactionService(plugin, repository)
        repository.failure = OptimisticLockingFailureException("stale")

        assertFailsWith<OptimisticLockingFailureException> {
            service.cancelAllApplicationsForPlayer(MfPlayer(MfPlayerId("applicant"), name = "Applicant"))
        }
    }

    @Test
    fun addedFieldsAreListedInOrder() {
        val service = MfFactionService(plugin, FakeRepository())
        val first = mock(MfFactionField::class.java)
        val second = mock(MfFactionField::class.java)

        service.addField(first)
        service.addField(second)

        assertEquals(listOf(first, second), service.fields)
    }
}
