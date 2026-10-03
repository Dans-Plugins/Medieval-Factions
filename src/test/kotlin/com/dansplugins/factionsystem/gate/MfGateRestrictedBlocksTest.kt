package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.BlockState
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.InputStreamReader
import java.util.UUID
import java.util.logging.Logger

/**
 * `gates.restrictedBlocks` (#2071): the bundled config.yml groups the list with YAML anchors, which makes it a list of
 * lists. The list must load from that structure, and the startup review must only warn about existing gates made of
 * restricted materials, never delete them.
 */
class MfGateRestrictedBlocksTest {

    private fun bundledConfig(): YamlConfiguration {
        val stream = javaClass.classLoader.getResourceAsStream("config.yml")
            ?: error("bundled config.yml not found on the test classpath")
        return InputStreamReader(stream, Charsets.UTF_8).use { YamlConfiguration.loadConfiguration(it) }
    }

    // --- loading the list ---

    @Test
    fun `bundled config yml is a list of lists, which getStringList reads as empty`() {
        val config = bundledConfig()
        // Documents the cause of #2071: the anchors stay in config.yml, so the reader has to flatten them.
        assertTrue(config.getList("gates.restrictedBlocks")!!.all { it is List<*> })
        assertEquals(emptyList<String>(), config.getStringList("gates.restrictedBlocks"))
    }

    @Test
    fun `flattenMaterialNames reads every material in the bundled config yml`() {
        val names = MfGateService.flattenMaterialNames(bundledConfig().getList("gates.restrictedBlocks")).toSet()
        listOf("SAND", "GRAVEL", "ANVIL", "WHITE_CONCRETE_POWDER", "WHITE_CARPET", "WHITE_BANNER", "WHITE_BED", "OAK_SIGN", "OAK_BUTTON")
            .forEach { assertTrue(it in names, "$it should be restricted") }
        // Every name in the bundled list is a real material.
        names.forEach { Material.valueOf(it) }
    }

    @Test
    fun `flattenMaterialNames accepts flat lists, nested lists and nulls`() {
        assertEquals(emptyList<String>(), MfGateService.flattenMaterialNames(null))
        assertEquals(listOf("SAND", "GRAVEL"), MfGateService.flattenMaterialNames(listOf("SAND", "GRAVEL")))
        assertEquals(
            listOf("SAND", "GRAVEL", "ANVIL", "TORCH"),
            MfGateService.flattenMaterialNames(listOf(listOf("SAND", listOf("GRAVEL")), "ANVIL", null, listOf("TORCH")))
        )
    }

    @Test
    fun `gate service loads restricted materials from the bundled config yml`() {
        val fixture = Fixture(bundledConfig(), emptyList())
        val restricted = fixture.gateService.restrictedBlockMaterials
        assertTrue(Material.SAND in restricted)
        assertTrue(Material.OAK_BUTTON in restricted)
        assertTrue(Material.STONE !in restricted)
        verify(fixture.logger).info("Loaded ${restricted.size} restricted block materials.")
    }

    // --- startup review: warn, never delete ---

    @Test
    fun `existing restricted-material gates survive the startup review and are named in a warning`() {
        val fixture = Fixture(YamlConfiguration(), emptyList())
        val sandGate = fixture.gate(Material.SAND)
        val stoneGate = fixture.gate(Material.STONE)
        val withGates = Fixture(bundledConfig(), listOf(sandGate, stoneGate))

        // Run every task the service scheduled at startup, exactly as the server would.
        withGates.runScheduledTasks()

        assertEquals(setOf(sandGate.id, stoneGate.id), withGates.gateService.gates.map { it.id }.toSet())
        // No write of any kind reached the repository: no delete, deleteAll or upsert.
        val writes = mockingDetails(withGates.gateRepo).invocations.map { it.method.name }
            .filter { it.startsWith("delete") || it.startsWith("upsert") }
        assertEquals(emptyList<String>(), writes)
        verify(withGates.logger).warning(contains(sandGate.id.value))
        verify(withGates.logger).warning(contains(sandGate.factionId.value))
        verify(withGates.logger).warning(contains("SAND"))
        verify(withGates.logger, never()).warning(contains(stoneGate.id.value))
        assertEquals(listOf(sandGate), withGates.gateService.warnAboutGatesWithRestrictedMaterials())
    }

    private class Fixture(config: FileConfiguration, existingGates: List<MfGate>) {
        val plugin: MedievalFactions = mock(MedievalFactions::class.java)
        val logger: Logger = mock(Logger::class.java)
        val gateRepo: MfGateRepository = mock(MfGateRepository::class.java)
        private val scheduler: BukkitScheduler = mock(BukkitScheduler::class.java)
        private val scheduled = mutableListOf<Runnable>()
        val gateService: MfGateService

        init {
            val server = mock(Server::class.java)
            `when`(plugin.logger).thenReturn(logger)
            `when`(plugin.config).thenReturn(config)
            `when`(plugin.server).thenReturn(server)
            `when`(server.scheduler).thenReturn(scheduler)
            `when`(scheduler.runTask(any(Plugin::class.java), any(Runnable::class.java))).thenAnswer {
                scheduled += it.getArgument<Runnable>(1)
                null
            }
            `when`(scheduler.runTaskAsynchronously(any(Plugin::class.java), any(Runnable::class.java))).thenAnswer {
                scheduled += it.getArgument<Runnable>(1)
                null
            }
            `when`(gateRepo.getGates()).thenReturn(existingGates)
            val blockSafety = MfGateBlockSafety(
                defaultBlockDataString = { "minecraft:${it.name.lowercase()}" },
                defaultBlockState = { mock(BlockState::class.java) }
            )
            gateService = MfGateService(plugin, gateRepo, mock(MfGateCreationContextRepository::class.java), blockSafety)
            val services = mock(Services::class.java)
            `when`(services.gateService).thenReturn(gateService)
            `when`(plugin.services).thenReturn(services)
        }

        fun runScheduledTasks() {
            scheduled.forEach { it.run() }
            verify(logger, never()).log(any(), anyString(), any(Throwable::class.java))
        }

        fun gate(material: Material): MfGate {
            val position = MfBlockPosition(UUID.randomUUID(), 0, 64, 0)
            return MfGate(
                plugin,
                factionId = MfFactionId.generate(),
                area = MfCuboidArea(position, position),
                trigger = position,
                material = material
            )
        }
    }
}
