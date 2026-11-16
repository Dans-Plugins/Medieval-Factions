package com.dansplugins.factionsystem.command.faction.set.description

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
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

class MfFactionSetDescriptionCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(DescriptionPrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionSetDescriptionNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionSetDescriptionOperationCancelled"])
                }
            }
        }

    private fun setOrContinueDescription(context: ConversationContext, input: String?): Prompt? {
        val conversable = context.forWhom
        if (conversable !is Player) return StringPrompt.END_OF_CONVERSATION
        if (input == null) return StringPrompt.END_OF_CONVERSATION
        if (input == plugin.language["EndSequence"]) {
            setFactionDescription(conversable, context.getSessionData("description") as? String ?: "")
            return StringPrompt.END_OF_CONVERSATION
        }
        context.setSessionData("description", ((context.getSessionData("description") as? String)?.plus(" ") ?: "") + input)
        return ContinueDescriptionPrompt()
    }

    private inner class DescriptionPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionSetDescriptionPrompt", plugin.language["EscapeSequence"], plugin.language["EndSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? = setOrContinueDescription(context, input)
    }

    private inner class ContinueDescriptionPrompt : StringPrompt() {
        override fun getPromptText(context: ConversationContext): String = plugin.language["CommandFactionSetDescriptionContinuePrompt", plugin.language["EscapeSequence"], plugin.language["EndSequence"]]
        override fun acceptInput(context: ConversationContext, input: String?): Prompt? = setOrContinueDescription(context, input)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.desc")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            conversationFactory.buildConversation(sender).begin()
            return true
        }
        // With mf.force.desc, `<faction> -- <description>` sets the description of the named faction (by ID or name,
        // quoted if it has spaces) without its role check (#1898). The `--` keeps every description a member could
        // already type (which may start with any word, including a faction name) meaning their own faction's.
        val separatorIndex = if (sender.hasPermission("mf.force.desc")) args.indexOf("--") else -1
        if (separatorIndex > 0) {
            val forcedFactionName = args.take(separatorIndex).toTypedArray().unquote().joinToString(" ")
            setFactionDescription(sender, args.drop(separatorIndex + 1).joinToString(" "), forcedFactionName)
            return true
        }
        setFactionDescription(sender, args.joinToString(" "))
        return true
    }

    private fun setFactionDescription(player: Player, description: String, forcedFactionName: String? = null) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction: MfFaction?
                if (forcedFactionName != null) {
                    faction = factionService.getFaction(MfFactionId(forcedFactionName)) ?: factionService.getFaction(forcedFactionName)
                    if (faction == null) {
                        player.sendMessage("$RED${plugin.language["CommandFactionForceInvalidFaction", forcedFactionName]}")
                        return@Runnable
                    }
                } else {
                    faction = factionService.getFaction(mfPlayer.id)
                    if (faction == null) {
                        player.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.changeDescription)) {
                        player.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                factionService.save(
                    faction.copy(
                        description = if (description.length <= 4096) description else description.substring(0, 4095) + "…"
                    )
                ).onFailure {
                    player.sendMessage("$RED${plugin.language["CommandFactionSetDescriptionFailedToSaveFaction"]}")
                    plugin.logger.log(Level.SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                if (forcedFactionName != null) {
                    player.sendMessage("$GREEN${plugin.language["CommandFactionForceSetDescriptionSuccess", faction.name, description]}")
                    plugin.logger.info("${player.name} set the description of ${faction.name} (${faction.id.value}) with mf.force.desc")
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["CommandFactionSetDescriptionSuccess", description]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        player.performCommand("faction info")
                    }
                )
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
