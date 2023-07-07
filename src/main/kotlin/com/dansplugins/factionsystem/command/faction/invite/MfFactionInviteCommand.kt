package com.dansplugins.factionsystem.command.faction.invite

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionInvite
import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.ValidatingPrompt
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionInviteCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(PlayerPrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionInviteNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionInviteOperationCancelled"])
                }
            }
        }

    private inner class PlayerPrompt : ValidatingPrompt() {
        override fun getPromptText(context: ConversationContext) =
            plugin.language["CommandFactionInvitePlayerPrompt", plugin.language["EscapeSequence"]]

        override fun isInputValid(context: ConversationContext, input: String): Boolean {
            val player = plugin.server.getOfflinePlayer(input)
            return player.isOnline || player.hasPlayedBefore()
        }

        override fun getFailedValidationText(context: ConversationContext, invalidInput: String) =
            "${BukkitChatColor.RED}${plugin.language["CommandFactionInviteInvalidTarget"]}"

        override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            invitePlayer(conversable, plugin.server.getOfflinePlayer(input))
            return END_OF_CONVERSATION
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.invite")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            conversationFactory.buildConversation(sender).begin()
            return true
        }
        val target = plugin.server.getOfflinePlayer(args[0])
        if (!target.isOnline && !target.hasPlayedBefore()) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteInvalidTarget"]}")
            return true
        }
        invitePlayer(sender, target)
        return true
    }

    private fun invitePlayer(sender: Player, target: OfflinePlayer) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.invite)) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteNoFactionPermission"]}")
                    return@Runnable
                }
                if (faction.invites.any { it.playerId == targetMfPlayer.id }) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteAlreadyInvited"]}")
                    return@Runnable
                }
                if (faction.members.any { it.playerId == targetMfPlayer.id }) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteAlreadyMember"]}")
                    return@Runnable
                }
                val maxMembers = plugin.config.getInt("factions.maxMembers")
                if (maxMembers > 0 && faction.members.size >= maxMembers) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteFactionFull"]}")
                    return@Runnable
                }
                factionService.save(
                    faction.copy(
                        invites = faction.invites + MfFactionInvite(targetMfPlayer.id)
                    )
                ).onFailure {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionInviteFailedToSaveFaction"]}")
                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("${BukkitChatColor.GREEN}${plugin.language["CommandFactionInviteSuccess", target.name ?: plugin.language["UnknownPlayer"]]}")
                val targetOnlinePlayer = target.player
                if (targetOnlinePlayer != null) {
                    targetOnlinePlayer.spigot().sendMessage(
                        *arrayOf(
                            TextComponent(plugin.language["CommandFactionInviteReceived", faction.name] + " ").apply {
                                color = SpigotChatColor.GRAY
                            },
                            TextComponent(plugin.language["CommandFactionInviteAccept", faction.name]).apply {
                                color = SpigotChatColor.GREEN
                                hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionInviteAcceptHover"]))
                                clickEvent = ClickEvent(RUN_COMMAND, "/faction join ${faction.id.value}")
                            }
                        )
                    )
                } else {
                    plugin.services.notificationService.sendNotification(
                        targetMfPlayer.id,
                        MfNotification(
                            plugin.language["CommandFactionInviteReceivedNotificationTitle", faction.name],
                            plugin.language["CommandFactionInviteReceivedNotificationBody", faction.name]
                        )
                    )
                }
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        sender.performCommand("faction info")
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
    ) = when {
        args.isEmpty() ->
            plugin.server.offlinePlayers
                .mapNotNull { it.name }
        args.size == 1 ->
            plugin.server.offlinePlayers
                .filter { it.name?.lowercase()?.startsWith(args[0].lowercase()) == true }
                .mapNotNull { it.name }
        else -> emptyList()
    }
}
