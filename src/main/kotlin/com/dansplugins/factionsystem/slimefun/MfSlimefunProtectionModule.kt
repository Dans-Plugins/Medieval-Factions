package com.dansplugins.factionsystem.slimefun

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.ProtectionModule
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * Medieval Factions' protection module for Slimefun (#1750). Slimefun asks every registered module
 * before a player uses or breaks one of its blocks, and refuses the action if any module says no.
 *
 * Every Slimefun class is referenced from this file only. It is loaded only when the Slimefun plugin
 * is enabled and `slimefun.protection.enabled` is true, so servers without Slimefun never load it.
 * Slimefun shades its protection API (dough) under its own package, which is why the imports point
 * into `io.github.thebusybiscuit.slimefun4.libraries`.
 */
class MfSlimefunProtectionModule(
    private val owningPlugin: Plugin,
    private val policy: MfSlimefunProtectionPolicy
) : ProtectionModule {

    override fun load() = Unit

    override fun getPlugin(): Plugin = owningPlugin

    override fun hasPermission(player: OfflinePlayer, location: Location, interaction: Interaction): Boolean =
        policy.isAllowed(player, location, toAction(interaction))

    companion object {

        /**
         * Registers the module with Slimefun's protection manager. Slimefun only registers a module for
         * a plugin that is enabled, which Medieval Factions is while its own `onEnable` runs.
         */
        fun register(plugin: Plugin, pluginManager: PluginManager, policy: MfSlimefunProtectionPolicy) {
            Slimefun.getProtectionManager().registerModule(pluginManager, plugin.name) { owningPlugin ->
                MfSlimefunProtectionModule(owningPlugin, policy)
            }
        }

        /**
         * Maps Slimefun's interaction onto [MfSlimefunAction] by name. An interaction added in a later
         * Slimefun version is treated as using a block, the most common restricted case.
         */
        internal fun toAction(interaction: Interaction): MfSlimefunAction =
            MfSlimefunAction.values().firstOrNull { it.name == interaction.name } ?: MfSlimefunAction.INTERACT_BLOCK
    }
}
