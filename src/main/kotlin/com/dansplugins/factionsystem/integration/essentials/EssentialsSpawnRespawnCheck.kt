package com.dansplugins.factionsystem.integration.essentials

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale

/**
 * Detects an EssentialsX Spawn setup that overrides `factions.respawnAtFactionHome`.
 *
 * EssentialsX Spawn sets the respawn location from a listener whose priority comes from
 * `respawn-listener-priority` in `plugins/Essentials/config.yml` (shipped as `high`). Medieval Factions sets
 * the faction home at LOW priority, so EssentialsX Spawn at LOW or above sets the location after it and the
 * player respawns at the EssentialsX spawn instead of the faction home. Only `none` (EssentialsX Spawn
 * leaves respawns alone) and `lowest` (it runs first) let the faction home apply. A missing or unrecognised
 * value counts as overriding, because EssentialsX then falls back to a priority above LOWEST.
 */
object EssentialsSpawnRespawnCheck {

    const val PRIORITY_KEY = "respawn-listener-priority"

    private val NON_OVERRIDING = setOf("none", "lowest")

    fun overridesFactionHome(respawnListenerPriority: String?): Boolean =
        respawnListenerPriority?.trim()?.lowercase(Locale.ROOT) !in NON_OVERRIDING

    /** Reads `respawn-listener-priority` from EssentialsX's config file, or null when the file is absent. */
    fun readPriority(essentialsConfigFile: File): String? {
        if (!essentialsConfigFile.isFile) return null
        return YamlConfiguration.loadConfiguration(essentialsConfigFile).getString(PRIORITY_KEY)
    }

    fun warning(respawnListenerPriority: String?): String =
        "factions.respawnAtFactionHome is on, but EssentialsX Spawn is installed with " +
            "$PRIORITY_KEY: ${respawnListenerPriority ?: "(not set)"}, so it sets the respawn location after " +
            "Medieval Factions and players respawn at the EssentialsX spawn instead of their faction home. " +
            "To use faction homes, set $PRIORITY_KEY to none (or lowest) in plugins/Essentials/config.yml " +
            "and restart."
}
