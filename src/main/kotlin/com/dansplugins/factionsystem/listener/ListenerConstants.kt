package com.dansplugins.factionsystem.listener

import org.bukkit.Material

/**
 * Shared constants used across listener classes
 */
object ListenerConstants {
    /**
     * Set of materials that are considered projectile weapons.
     * These items should be allowed to be used even when looking at blocks in protected territory,
     * as the action doesn't interact with the block itself.
     */
    val PROJECTILE_WEAPONS = setOf(
        Material.BOW,
        Material.CROSSBOW,
        Material.TRIDENT,
        Material.SNOWBALL,
        Material.EGG,
        Material.ENDER_PEARL,
        Material.SPLASH_POTION,
        Material.LINGERING_POTION
    )
}
