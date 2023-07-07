package com.dansplugins.factionsystem.command.faction.set.name

import com.dansplugins.factionsystem.MedievalFactions
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
import preponderous.ponder.command.unquote
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
                var faction: MfFaction? = null
                var name = ""
                if (hasForcePermission) {
                    val unquotedArgs = args.unquote()
                    if (unquotedArgs.size > 1) {
                        faction = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])
                        name = unquotedArgs.drop(1).joinToString(" ")
                    }
                }
                if (faction == null) {
                    faction = factionService.getFaction(mfPlayer.id)
                    name = args.joinToString(" ")
                }
                if (name.length > maxFactionNameLength) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameNameTooLong", maxFactionNameLength.toString()]}")
                    return@Runnable
                }
                if (faction == null) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.changeName)) {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetNameNoFactionPermission"]}")
                    return@Runnable
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
