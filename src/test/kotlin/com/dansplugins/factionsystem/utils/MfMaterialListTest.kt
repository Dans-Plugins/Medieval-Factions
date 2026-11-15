package com.dansplugins.factionsystem.utils

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/** Covers block and item tags in material lists such as `factions.wartimePlaceableBlocks` (#1967). */
class MfMaterialListTest {

    private val signs: Tag<Material> = taggedWith(Material.OAK_SIGN, Material.SPRUCE_WALL_SIGN)
    private val boats: Tag<Material> = taggedWith(Material.OAK_BOAT)
    private val blockTags = mapOf(NamespacedKey.minecraft("signs") to signs)
    private val itemTags = mapOf(NamespacedKey.minecraft("boats") to boats)
    private val lookup: (String, NamespacedKey) -> Tag<Material>? = { registry, key ->
        when (registry) {
            Tag.REGISTRY_BLOCKS -> blockTags[key]
            Tag.REGISTRY_ITEMS -> itemTags[key]
            else -> null
        }
    }

    @Test
    fun `material names match case-insensitively as before`() {
        val list = MfMaterialList(listOf("scaffolding", "TORCH"), lookup)

        assertTrue(Material.SCAFFOLDING in list)
        assertTrue(Material.TORCH in list)
        assertFalse(Material.OAK_SIGN in list)
    }

    @Test
    fun `a block tag matches every material in it`() {
        val list = MfMaterialList(listOf("#minecraft:signs"), lookup)

        assertTrue(Material.OAK_SIGN in list)
        assertTrue(Material.SPRUCE_WALL_SIGN in list)
        assertFalse(Material.TORCH in list)
    }

    @Test
    fun `a tag without a namespace is read as minecraft`() {
        val list = MfMaterialList(listOf("#SIGNS"), lookup)

        assertTrue(Material.OAK_SIGN in list)
    }

    @Test
    fun `an item tag matches too`() {
        val list = MfMaterialList(listOf("#minecraft:boats"), lookup)

        assertTrue(Material.OAK_BOAT in list)
    }

    @Test
    fun `an unknown tag matches nothing`() {
        val list = MfMaterialList(listOf("#minecraft:no_such_tag", "#not a key"), lookup)

        assertFalse(Material.OAK_SIGN in list)
    }

    @Test
    fun `names and tags can be mixed`() {
        val list = MfMaterialList(listOf("TORCH", "#signs"), lookup)

        assertTrue(Material.TORCH in list)
        assertTrue(Material.OAK_SIGN in list)
        assertFalse(Material.SCAFFOLDING in list)
    }

    private fun taggedWith(vararg materials: Material): Tag<Material> {
        @Suppress("UNCHECKED_CAST")
        return mock(Tag::class.java) { invocation ->
            invocation.method.name == "isTagged" && invocation.arguments.firstOrNull() in materials
        } as Tag<Material>
    }
}
