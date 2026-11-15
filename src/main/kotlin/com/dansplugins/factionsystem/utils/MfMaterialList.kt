package com.dansplugins.factionsystem.utils

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag

/**
 * A configured list of materials. Each entry is either a Material name (`OAK_DOOR`, case-insensitive)
 * or a Minecraft block or item tag written with a leading `#` (`#minecraft:signs`, or `#signs` for the
 * `minecraft` namespace), which matches every material in that tag, including tags from data packs.
 * An unknown tag matches nothing.
 */
class MfMaterialList internal constructor(
    entries: List<String>,
    private val tagLookup: (registry: String, key: NamespacedKey) -> Tag<Material>?
) {

    constructor(entries: List<String>) : this(
        entries,
        { registry, key -> Bukkit.getTag(registry, key, Material::class.java) }
    )

    private val materialNames: Set<String> = entries
        .filterNot { it.startsWith("#") }
        .mapTo(HashSet()) { it.uppercase() }

    private val tagKeys: List<NamespacedKey> = entries
        .filter { it.startsWith("#") }
        .mapNotNull { NamespacedKey.fromString(it.removePrefix("#").lowercase()) }

    operator fun contains(material: Material): Boolean {
        if (material.name in materialNames) return true
        return tagKeys.any { key ->
            tagLookup(Tag.REGISTRY_BLOCKS, key)?.isTagged(material) == true ||
                tagLookup(Tag.REGISTRY_ITEMS, key)?.isTagged(material) == true
        }
    }
}
