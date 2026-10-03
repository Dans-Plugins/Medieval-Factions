package com.dansplugins.factionsystem.command.faction.set.name

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType.ACTION_BAR
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionSetNameCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(NamePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionSetNameNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionSetNameOperationCancelled"])
                }
            }
        }

    private inner class NamePrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionSetNameNamePrompt", plugin.language["EscapeSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            if (input == null) return END_OF_CONVERSATION
            setFactionName(conversable, arrayOf(input))
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.rename")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetNameNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetNameNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            conversationFactory.buildConversation(sender).begin()
            return true
        }
        setFactionName(sender, args)
        return true
    }

    private fun setFactionName(player: Player, args: Array<out String>) {
        val onlinePlayers = plugin.server.onlinePlayers.associateWith { it.location.chunk }
        val hasForcePermission = player.hasPermission("mf.force.rename")
        val maxFactionNameLength = plugin.config.getInt("factions.maxNameLength")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["CommandFactionSetNameFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction: MfFaction?
                val name: String
                val isForced: Boolean
                // With mf.force.rename the shape of the arguments alone decides the target, never whether a faction
                // happens to exist: one (possibly quoted) argument renames the sender's own faction, while two or more
                // name a target faction (by ID or name, quoted if it has spaces) followed by the new name. A target
                // that does not resolve is refused rather than falling back to the sender's own faction, which would
                // rename it to the whole argument string (#2070), matching how /f kick and /f unclaimall treat an
                // unknown faction. Without the permission, every argument is the new name of the sender's own faction.
                val unquotedArgs = if (hasForcePermission) args.unquote() else emptyArray()
                if (unquotedArgs.size > 1) {
                    val targetFactionName = unquotedArgs[0]
                    faction = factionService.getFaction(MfFactionId(targetFactionName)) ?: factionService.getFaction(targetFactionName)
                    if (faction == null) {
                        player.sendMessage(
                            "$RED${plugin.language[
                                "CommandFactionSetNameInvalidFaction",
                                targetFactionName,
                                unquotedArgs.joinToString(" ")
                            ]}"
                        )
                        return@Runnable
                    }
                    name = unquotedArgs.drop(1).joinToString(" ")
                    isForced = true
                } else {
                    faction = factionService.getFaction(mfPlayer.id)
                    // A single quoted argument (/f set name "New Name") is unquoted; anything else keeps the raw text.
                    name = unquotedArgs.singleOrNull()?.takeIf { it.isNotBlank() } ?: args.joinToString(" ")
                    isForced = false
                }
                if (name.length > maxFactionNameLength) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameNameTooLong", maxFactionNameLength.toString()]}")
                    return@Runnable
                }
                if (faction == null) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameMustBeInAFaction"]}")
                    return@Runnable
                }
                // An explicitly named faction is only reachable with mf.force.rename, which replaces the faction's own
                // role check, matching how mf.force.flag is honoured.
                if (!isForced) {
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.changeName)) {
                        player.sendMessage("$RED${plugin.language["CommandFactionSetNameNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                if (factionService.getFaction(name) != null) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameFactionAlreadyExists"]}")
                    return@Runnable
                }
                val updatedFaction = factionService.save(faction.copy(name = name)).onFailure {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["CommandFactionSetNameSuccess", name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        player.performCommand("faction info")
                    }
                )
                val claimService = plugin.services.claimService
                onlinePlayers.filter { (_, chunk) -> claimService.getClaim(chunk)?.factionId == updatedFaction.id }
                    .forEach { (player, _) ->
                        val title = "${ChatColor.of(updatedFaction.flags[plugin.flags.color])}${updatedFaction.name}"
                        val subtitle = "${ChatColor.of(updatedFaction.flags[plugin.flags.color])}${updatedFaction.description}"
                        if (plugin.config.getBoolean("factions.titleTerritoryIndicator")) {
                            player.resetTitle()
                            player.sendTitle(
                                title,
                                subtitle,
                                plugin.config.getInt("factions.titleTerritoryFadeInLength"),
                                plugin.config.getInt("factions.titleTerritoryDuration"),
                                plugin.config.getInt("factions.titleTerritoryFadeOutLength")
                            )
                        }
                        if (plugin.config.getBoolean("factions.actionBarTerritoryIndicator")) {
                            player.spigot().sendMessage(ACTION_BAR, *TextComponent.fromLegacyText(title))
                        }
                    }
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
