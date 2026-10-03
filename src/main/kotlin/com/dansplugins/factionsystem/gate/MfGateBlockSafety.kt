package com.dansplugins.factionsystem.gate

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.TileState
import org.bukkit.inventory.InventoryHolder

/**
 * Decides whether a block can safely be part of a gate.
 *
 * A gate stores only a [Material]. Opening a gate replaces its blocks with air, and closing it puts them back
 * with `block.type = material`, which places the material's *default* block data and an empty block entity.
 * Anything else the original block held is therefore lost:
 *
 * - **Block entities** (chests, barrels, hoppers, furnaces, shulker boxes, signs, banners, beehives, ...) lose
 *   their contents/data when the gate opens. For containers this destroys items, so such blocks are refused.
 * - **Player-chosen block state** (stair half/facing, slab type, log axis, door hinge, leaf persistence, ...)
 *   comes back as the default (e.g. top stairs become bottom stairs, issue #1255), so blocks whose
 *   player-chosen state is not the default are refused.
 *
 * State that the game derives from neighbouring blocks (fence/pane/wall connections, stair shape, leaf
 * distance, snowy, waterlogged, power) is ignored, so classic portcullis materials such as iron bars and
 * fences, and plain blocks such as stone, are still accepted.
 *
 * The block data comparison is done on [org.bukkit.block.data.BlockData.getAsString] strings
 * (`minecraft:oak_stairs[facing=north,half=top,...]`) so that the rules are independent of the server version.
 */
class MfGateBlockSafety(
    private val defaultBlockDataString: (Material) -> String = { it.createBlockData().asString },
    private val defaultBlockState: (Material) -> BlockState? = { it.createBlockData().createBlockState() }
) {

    sealed interface Problem {
        /** The block is a block entity (container or other data-holding block). */
        data class DataHoldingBlock(val material: Material) : Problem

        /** The block has player-chosen state that differs from the default; [properties] are the differing ones. */
        data class NonDefaultState(val material: Material, val properties: Map<String, String>) : Problem
    }

    /**
     * Checks a block in the world. Must be called on the main thread, since it reads the block state.
     * Returns null when the block is safe to use in a gate.
     */
    fun check(block: Block): Problem? {
        val material = block.type
        if (isDataHoldingState(block.state)) return Problem.DataHoldingBlock(material)
        val differing = nonDefaultPlayerChosenProperties(block.blockData.asString, defaultBlockDataString(material))
        if (differing.isNotEmpty()) return Problem.NonDefaultState(material, differing)
        return null
    }

    /**
     * Material-level check used for gates that already exist (where no block can be inspected, because the gate
     * may be open or unloaded). Returns null if it cannot be determined on this server version.
     */
    fun isDataHoldingMaterial(material: Material): Boolean? {
        return try {
            isDataHoldingState(defaultBlockState(material))
        } catch (e: Exception) {
            null
        } catch (e: LinkageError) {
            // BlockData#createBlockState is not available on older server versions
            null
        }
    }

    private fun isDataHoldingState(state: BlockState?) = state is TileState || state is InventoryHolder

    companion object {

        /**
         * Block state properties whose value is derived by the game from neighbouring blocks or the environment,
         * rather than chosen by the player when placing the block. Differences in these are not refused.
         */
        val NEIGHBOUR_DERIVED_PROPERTIES = setOf(
            // connections: fences, panes, iron bars, walls, redstone wire, mushroom blocks, chorus plants
            "north",
            "east",
            "south",
            "west",
            "up",
            "down",
            // stair shape follows neighbouring stairs
            "shape",
            // leaves / scaffolding distance to support
            "distance",
            // grass, podzol, mycelium under snow
            "snowy",
            "waterlogged",
            "power",
            "powered",
            "in_wall"
        )

        /**
         * Parses the property section of a block data string such as `minecraft:oak_stairs[facing=north,half=top]`.
         */
        fun parseProperties(blockDataString: String): Map<String, String> {
            val start = blockDataString.indexOf('[')
            val end = blockDataString.lastIndexOf(']')
            if (start == -1 || end <= start) return emptyMap()
            return blockDataString.substring(start + 1, end)
                .split(',')
                .mapNotNull { property ->
                    val separator = property.indexOf('=')
                    if (separator == -1) return@mapNotNull null
                    property.substring(0, separator).trim() to property.substring(separator + 1).trim()
                }
                .toMap()
        }

        /**
         * Returns the player-chosen properties of [actual] whose values differ from [default].
         */
        fun nonDefaultPlayerChosenProperties(actual: String, default: String): Map<String, String> {
            val defaultProperties = parseProperties(default)
            return parseProperties(actual).filter { (key, value) ->
                key !in NEIGHBOUR_DERIVED_PROPERTIES && defaultProperties[key] != value
            }
        }
    }
}
