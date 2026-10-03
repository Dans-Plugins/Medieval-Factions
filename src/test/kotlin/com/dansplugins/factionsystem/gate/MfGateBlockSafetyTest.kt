package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.Chest
import org.bukkit.block.Furnace
import org.bukkit.block.Hopper
import org.bukkit.block.ShulkerBox
import org.bukkit.block.Sign
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

class MfGateBlockSafetyTest {

    // Default block data strings as produced by Material#createBlockData().getAsString() on a 1.21 server
    private val defaults = mapOf(
        Material.STONE to "minecraft:stone",
        Material.OAK_PLANKS to "minecraft:oak_planks",
        Material.OAK_STAIRS to "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
        Material.OAK_SLAB to "minecraft:oak_slab[type=bottom,waterlogged=false]",
        Material.OAK_LOG to "minecraft:oak_log[axis=y]",
        Material.OAK_DOOR to "minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]",
        Material.OAK_LEAVES to "minecraft:oak_leaves[distance=7,persistent=false,waterlogged=false]",
        Material.IRON_BARS to "minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]",
        Material.OAK_FENCE to "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]",
        Material.COBBLESTONE_WALL to "minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]",
        Material.CHEST to "minecraft:chest[facing=north,type=single,waterlogged=false]",
        Material.BARREL to "minecraft:barrel[facing=north,open=false]",
        Material.HOPPER to "minecraft:hopper[enabled=true,facing=down]",
        Material.FURNACE to "minecraft:furnace[facing=north,lit=false]",
        Material.SHULKER_BOX to "minecraft:shulker_box[facing=up]",
        Material.OAK_SIGN to "minecraft:oak_sign[rotation=0,waterlogged=false]"
    )

    private val uut = MfGateBlockSafety(
        defaultBlockDataString = { defaults.getValue(it) },
        defaultBlockState = { material -> stateFor(material) }
    )

    private fun stateFor(material: Material): BlockState = when (material) {
        Material.CHEST -> mock(Chest::class.java)
        Material.BARREL -> mock(Barrel::class.java)
        Material.HOPPER -> mock(Hopper::class.java)
        Material.FURNACE -> mock(Furnace::class.java)
        Material.SHULKER_BOX -> mock(ShulkerBox::class.java)
        Material.OAK_SIGN -> mock(Sign::class.java)
        else -> mock(BlockState::class.java)
    }

    private fun block(material: Material, blockDataString: String = defaults.getValue(material)): Block {
        val block = mock(Block::class.java)
        val blockData = mock(BlockData::class.java)
        `when`(blockData.asString).thenReturn(blockDataString)
        `when`(block.type).thenReturn(material)
        `when`(block.blockData).thenReturn(blockData)
        `when`(block.state).thenReturn(stateFor(material))
        return block
    }

    // --- containers and other block entities are refused ---

    @Test
    fun `containers are refused`() {
        listOf(Material.CHEST, Material.BARREL, Material.HOPPER, Material.FURNACE, Material.SHULKER_BOX).forEach { material ->
            val problem = uut.check(block(material))
            assertEquals(MfGateBlockSafety.Problem.DataHoldingBlock(material), problem, "$material should be refused")
        }
    }

    @Test
    fun `non-container block entities such as signs are refused`() {
        assertEquals(MfGateBlockSafety.Problem.DataHoldingBlock(Material.OAK_SIGN), uut.check(block(Material.OAK_SIGN)))
    }

    // --- player-chosen non-default state is refused ---

    @Test
    fun `top stairs are refused (issue 1255)`() {
        val problem = uut.check(
            block(Material.OAK_STAIRS, "minecraft:oak_stairs[facing=north,half=top,shape=straight,waterlogged=false]")
        )
        assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_STAIRS, mapOf("half" to "top")), problem)
    }

    @Test
    fun `stairs facing another direction are refused`() {
        val problem = uut.check(
            block(Material.OAK_STAIRS, "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
        )
        assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_STAIRS, mapOf("facing" to "east")), problem)
    }

    @Test
    fun `top and double slabs are refused`() {
        listOf("top", "double").forEach { type ->
            val problem = uut.check(block(Material.OAK_SLAB, "minecraft:oak_slab[type=$type,waterlogged=false]"))
            assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_SLAB, mapOf("type" to type)), problem)
        }
    }

    @Test
    fun `sideways logs are refused`() {
        val problem = uut.check(block(Material.OAK_LOG, "minecraft:oak_log[axis=x]"))
        assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_LOG, mapOf("axis" to "x")), problem)
    }

    @Test
    fun `upper door halves are refused`() {
        val problem = uut.check(
            block(Material.OAK_DOOR, "minecraft:oak_door[facing=north,half=upper,hinge=left,open=false,powered=false]")
        )
        assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_DOOR, mapOf("half" to "upper")), problem)
    }

    @Test
    fun `player-placed leaves are refused because they would decay once restored`() {
        val problem = uut.check(
            block(Material.OAK_LEAVES, "minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]")
        )
        assertEquals(MfGateBlockSafety.Problem.NonDefaultState(Material.OAK_LEAVES, mapOf("persistent" to "true")), problem)
    }

    // --- plain and faithfully-restorable blocks are still accepted ---

    @Test
    fun `plain blocks are accepted`() {
        assertNull(uut.check(block(Material.STONE)))
        assertNull(uut.check(block(Material.OAK_PLANKS)))
    }

    @Test
    fun `stateful blocks in their default state are accepted`() {
        assertNull(uut.check(block(Material.OAK_STAIRS)))
        assertNull(uut.check(block(Material.OAK_SLAB)))
        assertNull(uut.check(block(Material.OAK_LOG)))
    }

    @Test
    fun `neighbour-derived connections are ignored so portcullis materials are accepted`() {
        assertNull(
            uut.check(block(Material.IRON_BARS, "minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]"))
        )
        assertNull(
            uut.check(block(Material.OAK_FENCE, "minecraft:oak_fence[east=true,north=false,south=false,waterlogged=true,west=true]"))
        )
        assertNull(
            uut.check(
                block(
                    Material.COBBLESTONE_WALL,
                    "minecraft:cobblestone_wall[east=low,north=none,south=none,up=false,waterlogged=false,west=tall]"
                )
            )
        )
        assertNull(
            uut.check(block(Material.OAK_STAIRS, "minecraft:oak_stairs[facing=north,half=bottom,shape=outer_left,waterlogged=false]"))
        )
    }

    // --- property parsing ---

    @Test
    fun `parseProperties handles blocks without properties`() {
        assertEquals(emptyMap<String, String>(), MfGateBlockSafety.parseProperties("minecraft:stone"))
        assertEquals(
            mapOf("axis" to "y"),
            MfGateBlockSafety.parseProperties("minecraft:oak_log[axis=y]")
        )
    }

    // --- material-level check for existing gates ---

    @Test
    fun `isDataHoldingMaterial detects container materials`() {
        assertEquals(true, uut.isDataHoldingMaterial(Material.CHEST))
        assertEquals(true, uut.isDataHoldingMaterial(Material.SHULKER_BOX))
        assertEquals(false, uut.isDataHoldingMaterial(Material.STONE))
        assertEquals(false, uut.isDataHoldingMaterial(Material.OAK_STAIRS))
    }

    @Test
    fun `isDataHoldingMaterial returns null when the server cannot tell`() {
        val oldServer = MfGateBlockSafety(
            defaultBlockDataString = { "" },
            defaultBlockState = { throw NoSuchMethodError("createBlockState") }
        )
        assertNull(oldServer.isDataHoldingMaterial(Material.CHEST))
    }

    // --- startup warning for gates that already exist ---

    @Test
    fun `existing container gates are loaded, kept and named in a warning`() {
        val plugin = mock(MedievalFactions::class.java)
        val logger = mock(Logger::class.java)
        val config = mock(FileConfiguration::class.java)
        val server = mock(Server::class.java)
        val scheduler = mock(BukkitScheduler::class.java)
        `when`(plugin.logger).thenReturn(logger)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(scheduler)
        `when`(config.getStringList("gates.restrictedBlocks")).thenReturn(emptyList())

        val position = MfBlockPosition(UUID.randomUUID(), 0, 64, 0)
        fun gate(material: Material) = MfGate(
            plugin,
            factionId = MfFactionId.generate(),
            area = MfCuboidArea(position, position),
            trigger = position,
            material = material
        )
        val chestGate = gate(Material.CHEST)
        val stoneGate = gate(Material.STONE)
        val gateRepo = mock(MfGateRepository::class.java)
        `when`(gateRepo.getGates()).thenReturn(listOf(chestGate, stoneGate))

        val gateService = MfGateService(plugin, gateRepo, mock(MfGateCreationContextRepository::class.java), uut)

        assertEquals(listOf(chestGate), gateService.warnAboutGatesWithDataHoldingMaterials())
        assertEquals(2, gateService.gates.size)
        verify(logger).warning(contains(chestGate.id.value))
        verify(logger, never()).warning(contains(stoneGate.id.value))
        verify(gateRepo, never()).delete(chestGate.id)
        verify(gateRepo, never()).upsert(chestGate)
    }
}
