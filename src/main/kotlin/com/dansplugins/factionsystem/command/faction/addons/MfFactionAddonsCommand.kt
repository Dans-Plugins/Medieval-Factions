package com.dansplugins.factionsystem.command.faction.addons

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.addon.MfAddonDetector
import com.dansplugins.factionsystem.addon.MfAddonKind
import com.dansplugins.factionsystem.addon.MfAddonStatus
import com.dansplugins.factionsystem.addon.MfDetectedAddon
import com.dansplugins.factionsystem.addon.MfKnownAddons
import com.dansplugins.factionsystem.addon.normalizeVersion
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.WHITE
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * `/mf addons`: lists the known add-ons and related plugins, whether each is installed, and
 * what is known about its compatibility. Reads only the bundled registry and the local plugin
 * manager; makes no network call.
 */
class MfFactionAddonsCommand(
    private val plugin: MedievalFactions,
    private val detectorFactory: () -> MfAddonDetector = { MfAddonDetector.forPluginManager(plugin.server.pluginManager) }
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAddonsNoPermission"]}")
            return true
        }
        val suggestions = plugin.config.getBoolean("addons.suggestions")
        val detected = detectorFactory().detect()
        // With suggestions off, plugins that are not installed are not advertised.
        val shown = if (suggestions) detected else detected.filter { it.isInstalled }

        sender.sendMessage("$AQUA${plugin.language["CommandFactionAddonsTitle", plugin.description.version]}")
        if (shown.isEmpty()) {
            sender.sendMessage("$GRAY${plugin.language["CommandFactionAddonsNoneInstalled"]}")
        }
        sendSection(sender, shown.filter { it.addon.kind == MfAddonKind.EXPANSION }, "CommandFactionAddonsExpansions")
        sendSection(sender, shown.filter { it.addon.kind == MfAddonKind.RELATED }, "CommandFactionAddonsRelated")
        if (!suggestions) {
            sender.sendMessage("$GRAY${plugin.language["CommandFactionAddonsSuggestionsOff"]}")
        }
        return true
    }

    private fun sendSection(sender: CommandSender, entries: List<MfDetectedAddon>, headingKey: String) {
        if (entries.isEmpty()) return
        sender.sendMessage("$YELLOW${plugin.language[headingKey]}")
        entries.forEach { entry ->
            sender.sendMessage(statusLine(entry))
            sender.sendMessage("$GRAY  ${plugin.language["CommandFactionAddonsDetail", entry.addon.description, entry.addon.url]}")
        }
    }

    internal fun statusLine(entry: MfDetectedAddon): String {
        val name = entry.addon.displayName
        val verified = entry.addon.verifiedVersion?.let(::normalizeVersion)
        val installed = entry.installedVersion?.let(::normalizeVersion)
        return when (entry.status) {
            MfAddonStatus.INSTALLED_VERIFIED ->
                "$GREEN${plugin.language["CommandFactionAddonsInstalledVerified", name, installed!!, MfKnownAddons.VERIFIED_WITH_MF]}"
            MfAddonStatus.INSTALLED_NOT_VERIFIED ->
                "$YELLOW${plugin.language["CommandFactionAddonsInstalledNotVerified", name, installed!!, MfKnownAddons.VERIFIED_WITH_MF, verified!!]}"
            MfAddonStatus.INSTALLED_NO_DATA ->
                "$WHITE${plugin.language["CommandFactionAddonsInstalledNoData", name, installed!!]}"
            MfAddonStatus.NOT_INSTALLED ->
                if (verified != null) {
                    "$GRAY${plugin.language["CommandFactionAddonsNotInstalledVerified", name, MfKnownAddons.VERIFIED_WITH_MF, verified]}"
                } else {
                    "$GRAY${plugin.language["CommandFactionAddonsNotInstalled", name]}"
                }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()

    companion object {
        const val PERMISSION = "mf.addons"
    }
}
