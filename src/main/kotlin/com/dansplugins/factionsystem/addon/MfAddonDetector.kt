package com.dansplugins.factionsystem.addon

import com.dansplugins.factionsystem.lang.Language
import org.bukkit.plugin.PluginManager

/** What is known about one registry entry on this server. */
enum class MfAddonStatus {
    NOT_INSTALLED,

    /** Installed, and its version equals the version recorded as verified. */
    INSTALLED_VERIFIED,

    /** Installed, but its version differs from the version recorded as verified. */
    INSTALLED_NOT_VERIFIED,

    /** Installed, and no compatibility evidence is recorded for it (related plugins). */
    INSTALLED_NO_DATA
}

data class MfDetectedAddon(
    val addon: MfKnownAddon,
    /** The version of the enabled plugin, or null when it is not installed or not enabled. */
    val installedVersion: String?
) {
    val isInstalled get() = installedVersion != null

    val status: MfAddonStatus
        get() = when {
            installedVersion == null -> MfAddonStatus.NOT_INSTALLED
            addon.verifiedVersion == null -> MfAddonStatus.INSTALLED_NO_DATA
            normalizeVersion(installedVersion) == normalizeVersion(addon.verifiedVersion) -> MfAddonStatus.INSTALLED_VERIFIED
            else -> MfAddonStatus.INSTALLED_NOT_VERIFIED
        }
}

/** Strips surrounding whitespace and one leading "v", so "v3.0.0" and "3.0.0" compare equal. */
fun normalizeVersion(version: String): String = version.trim().removePrefix("v").removePrefix("V")

/**
 * Detects which [MfKnownAddons] are enabled.
 *
 * @param enabledPluginVersion returns the version of the enabled plugin with the given
 * plugin.yml name, or null when no such plugin is enabled
 */
class MfAddonDetector(
    private val enabledPluginVersion: (String) -> String?,
    private val registry: List<MfKnownAddon> = MfKnownAddons.all
) {

    fun detect(): List<MfDetectedAddon> = registry.map { addon ->
        MfDetectedAddon(addon, enabledPluginVersion(addon.pluginName))
    }

    fun installed(): List<MfDetectedAddon> = detect().filter { it.isInstalled }

    /**
     * The data for the `installed_addons` AdvancedPie: one key per installed entry, each
     * with value 1, or {"none": 1} when nothing from the registry is installed.
     */
    fun installedAddonsChartData(): Map<String, Int> {
        val installed = installed()
        if (installed.isEmpty()) return mapOf(NONE_KEY to 1)
        return installed.associate { it.addon.displayName to 1 }
    }

    companion object {
        const val NONE_KEY = "none"

        fun forPluginManager(pluginManager: PluginManager) = MfAddonDetector({ name ->
            pluginManager.getPlugin(name)?.takeIf { it.isEnabled }?.description?.version
        })
    }
}

/**
 * The one console line logged at startup: the detected add-ons by name, or the
 * "none; see /mf addons" pointer.
 */
fun startupAddonsLine(detected: List<MfDetectedAddon>, language: Language): String {
    val installed = detected.filter { it.isInstalled }
    return if (installed.isEmpty()) {
        language["AddonsStartupNone"]
    } else {
        language["AddonsStartupDetected", installed.joinToString { "${it.addon.displayName} v${normalizeVersion(it.installedVersion!!)}" }]
    }
}
