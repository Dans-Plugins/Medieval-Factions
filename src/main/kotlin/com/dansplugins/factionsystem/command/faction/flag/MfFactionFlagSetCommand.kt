package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValidationFailure
import com.dansplugins.factionsystem.faction.flag.MfFlagValidationSuccess
import com.dansplugins.factionsystem.faction.flag.MfFlagValueCoercionFailure
import com.dansplugins.factionsystem.faction.flag.MfFlagValueCoercionSuccess
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
import org.bukkit.conversations.ValidatingPrompt
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionFlagSetCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val conversationFactory = ConversationFactory(plugin)
        .withModality(true)
        .withFirstPrompt(ValuePrompt())
        .withEscapeSequence(plugin.language["EscapeSequence"])
        .withLocalEcho(false)
        .thatExcludesNonPlayersWithMessage(plugin.language["CommandFactionFlagSetNotAPlayer"])
        .addConversationAbandonedListener { event ->
            if (!event.gracefulExit()) {
                val conversable = event.context.forWhom
                if (conversable is Player) {
                    conversable.sendMessage(plugin.language["CommandFactionFlagSetOperationCancelled"])
                }
            }
        }

    private inner class ValuePrompt : ValidatingPrompt() {
        override fun getPromptText(context: ConversationContext): String {
            val flag = context.getSessionData("flag") as? MfFlag<Any>
            return plugin.language["CommandFactionFlagSetValuePrompt", flag?.name ?: plugin.language["UnknownFlag"], plugin.language["EscapeSequence"]]
        }

        override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt? {
            val conversable = context.forWhom
            if (conversable !is Player) return END_OF_CONVERSATION
            val flag = context.getSessionData("flag") as? MfFlag<Any> ?: return END_OF_CONVERSATION
            val page = context.getSessionData("page") as? Int?
            setFlagValue(conversable, flag, input, page)
            return END_OF_CONVERSATION
        }

        override fun getFailedValidationText(context: ConversationContext, invalidInput: String): String {
            val flag = context.getSessionData("flag") as MfFlag<Any>
            return when (val coercionResult = flag.coerce(invalidInput)) {
                is MfFlagValueCoercionFailure -> "$RED${plugin.language[
                    "CommandFactionFlagSetValueCoercionFailed",
                    flag.type.simpleName ?: plugin.language["UnknownFlagType"]
                ]}: ${coercionResult.failureMessage}"
                is MfFlagValueCoercionSuccess<*> -> {
                    when (val validationResult = flag.validate(coercionResult.value)) {
                        is MfFlagValidationFailure -> "$RED${plugin.language["CommandFactionFlagSetValueValidationFailed"]}: ${validationResult.failureMessage}"
                        is MfFlagValidationSuccess -> "$RED${plugin.language["CommandFactionFlagSetValueUnknownValidationFailure"]}"
                    }
                }
            }
        }

        override fun isInputValid(context: ConversationContext, input: String): Boolean {
            val flag = context.getSessionData("flag") as MfFlag<Any>
            val coercionResult = flag.coerce(input)
            if (coercionResult !is MfFlagValueCoercionSuccess<*>) return false
            val validationResult = flag.validate(coercionResult.value)
            return validationResult is MfFlagValidationSuccess
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.flag.set")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetUsage"]}")
            return true
        }
        val flag = plugin.flags.get<Any>(args[0])
        if (flag == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetInvalidFlag"]}")
            return true
        }
        var lastArgOffset = 0
        val returnPage = if (args.last().startsWith("p=")) {
            lastArgOffset = 1
            args.last().substring("p=".length).toIntOrNull()
        } else {
            null
        }
        if (args.size - lastArgOffset < 2) {
            val conversation = conversationFactory.buildConversation(sender)
            conversation.context.setSessionData("page", returnPage)
            conversation.context.setSessionData("flag", flag)
            conversation.begin()
            return true
        }
        val flagValue = args.drop(1).dropLast(lastArgOffset).joinToString(" ")
        setFlagValue(sender, flag, flagValue, returnPage)
        return true
    }

    private fun setFlagValue(sender: Player, flag: MfFlag<Any>, flagValue: String, page: Int? = null) {
        val allowNeutrality = plugin.config.getBoolean("factions.allowNeutrality")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.setFlag(flag))) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNoFactionPermission"]}")
                    return@Runnable
                }
                when (val coercionResult = flag.coerce(flagValue)) {
                    is MfFlagValueCoercionFailure -> sender.sendMessage(
                        "$RED${plugin.language[
                            "CommandFactionFlagSetValueCoercionFailed",
                            flag.type.simpleName ?: plugin.language["UnknownFlagType"]
                        ]}: ${coercionResult.failureMessage}"
                    )
                    is MfFlagValueCoercionSuccess<*> -> {
                        when (val validationResult = flag.validate(coercionResult.value)) {
                            is MfFlagValidationFailure -> sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetValueValidationFailed"]}: ${validationResult.failureMessage}")
                            is MfFlagValidationSuccess -> {
                                if (flag == plugin.flags.isNeutral && coercionResult.value == true && !allowNeutrality) {
                                    sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNeutralityDisabled"]}")
                                    return@Runnable
                                }
                                factionService.save(faction.copy(flags = faction.flags + (flag to coercionResult.value))).onFailure {
                                    sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetFailedToSaveFaction"]}")
                                    plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                                    return@Runnable
                                }
                                sender.sendMessage("$GREEN${plugin.language["CommandFactionFlagSetSuccess", flag.name, coercionResult.value.toString()]}")
                                plugin.server.scheduler.runTask(
                                    plugin,
                                    Runnable {
                                        sender.performCommand("faction flag list" + if (page != null) " $page" else "")
                                    }
                                )
                            }
                        }
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
    ) = when {
        args.isEmpty() -> plugin.flags.map { it.name }
        args.size == 1 -> plugin.flags.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
        else -> emptyList()
    }
}
