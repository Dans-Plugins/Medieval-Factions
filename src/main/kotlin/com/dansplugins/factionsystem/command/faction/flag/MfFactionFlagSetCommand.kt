package com.dansplugins.factionsystem.command.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValidationFailure
import com.dansplugins.factionsystem.faction.flag.MfFlagValidationSuccess
import com.dansplugins.factionsystem.faction.flag.MfFlagValueCoercionFailure
import com.dansplugins.factionsystem.faction.flag.MfFlagValueCoercionSuccess
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.valueOrNull
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
import preponderous.ponder.command.unquote
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
            val targetFaction = context.getSessionData("targetFaction") as? MfFaction
            setFlagValue(conversable, targetFaction, flag, input, page)
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

        val hasForcePermission = sender.hasPermission("mf.force.flag")
        val parsedCommand = parseCommandArguments(args, hasForcePermission)

        val flag = plugin.flags.get<Any>(parsedCommand.flagName)
        if (flag == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetInvalidFlag"]}")
            return true
        }

        if (parsedCommand.flagValue == null) {
            startConversation(sender, flag, parsedCommand.targetFaction, parsedCommand.returnPage)
        } else {
            setFlagValue(sender, parsedCommand.targetFaction, flag, parsedCommand.flagValue, parsedCommand.returnPage)
        }
        return true
    }

    private data class ParsedCommand(
        val targetFaction: MfFaction?,
        val flagName: String,
        val flagValue: String?,
        val returnPage: Int?
    )

    private fun parseCommandArguments(args: Array<out String>, hasForcePermission: Boolean): ParsedCommand {
        val unquotedArgs = args.unquote()
        val lastArgOffset = if (args.isNotEmpty() && args.last().startsWith("p=")) 1 else 0
        val returnPage = if (args.isNotEmpty() && args.last().startsWith("p=")) {
            args.last().substring("p=".length).toIntOrNull()
        } else {
            null
        }

        var targetFaction: MfFaction? = null
        val flagName: String
        val flagValueArgs: List<String>

        if (hasForcePermission && unquotedArgs.size >= 2) {
            val factionService = plugin.services.factionService
            val potentialFaction = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])

            // Disambiguation strategy:
            // - If first arg is a valid faction AND we have at least 3 args total, treat it as: [faction] [flag] [value]
            // - Otherwise, treat it as: [flag] [value] (operating on own faction)
            // This prevents ambiguity when a faction name coincidentally matches a flag name
            if (potentialFaction != null && unquotedArgs.size >= 3) {
                targetFaction = potentialFaction
                flagName = unquotedArgs[1]
                flagValueArgs = unquotedArgs.drop(2).dropLast(lastArgOffset)
            } else {
                flagName = unquotedArgs[0]
                flagValueArgs = unquotedArgs.drop(1).dropLast(lastArgOffset)
            }
        } else {
            flagName = unquotedArgs[0]
            flagValueArgs = unquotedArgs.drop(1).dropLast(lastArgOffset)
        }

        val flagValue = if (flagValueArgs.isEmpty()) null else flagValueArgs.joinToString(" ")
        return ParsedCommand(targetFaction, flagName, flagValue, returnPage)
    }

    private fun startConversation(sender: Player, flag: MfFlag<Any>, targetFaction: MfFaction?, returnPage: Int?) {
        val conversation = conversationFactory.buildConversation(sender)
        conversation.context.setSessionData("page", returnPage)
        conversation.context.setSessionData("flag", flag)
        conversation.context.setSessionData("targetFaction", targetFaction)
        conversation.begin()
    }

    private fun setFlagValue(sender: Player, targetFaction: MfFaction?, flag: MfFlag<Any>, flagValue: String, page: Int? = null) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { executeFlagUpdate(sender, targetFaction, flag, flagValue, page) })
    }

    private fun executeFlagUpdate(sender: Player, targetFaction: MfFaction?, flag: MfFlag<Any>, flagValue: String, page: Int?) {
        val mfPlayer = getOrSavePlayer(sender) ?: return
        val factionService = plugin.services.factionService
        val faction = resolveFactionToModify(sender, mfPlayer, targetFaction, factionService) ?: return

        if (!checkSetPermissions(sender, mfPlayer, faction, targetFaction, flag)) return

        processAndSaveFlagValue(sender, faction, flag, flagValue, targetFaction, page)
    }

    private fun getOrSavePlayer(sender: Player): MfPlayer? {
        val playerService = plugin.services.playerService
        val player = playerService.getPlayer(sender)
        if (player != null) return player

        val saveResult = playerService.save(MfPlayer(plugin, sender))

        saveResult.onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetFailedToSavePlayer"]}")
            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
            return null
        }

        return saveResult.valueOrNull()
    }

    private fun resolveFactionToModify(
        sender: Player,
        mfPlayer: MfPlayer,
        targetFaction: MfFaction?,
        factionService: com.dansplugins.factionsystem.faction.MfFactionService
    ): MfFaction? {
        return if (targetFaction != null) {
            targetFaction
        } else {
            factionService.getFaction(mfPlayer.id) ?: run {
                sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetMustBeInAFaction"]}")
                null
            }
        }
    }

    private fun checkSetPermissions(
        sender: Player,
        mfPlayer: MfPlayer,
        faction: MfFaction,
        targetFaction: MfFaction?,
        flag: MfFlag<Any>
    ): Boolean {
        if (targetFaction != null) {
            if (!sender.hasPermission("mf.force.flag")) {
                sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNoPermission"]}")
                return false
            }
        } else {
            val role = faction.getRole(mfPlayer.id)
            if (role == null || !role.hasPermission(faction, plugin.factionPermissions.setFlag(flag))) {
                sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNoFactionPermission"]}")
                return false
            }
        }
        return true
    }

    private fun processAndSaveFlagValue(
        sender: Player,
        faction: MfFaction,
        flag: MfFlag<Any>,
        flagValue: String,
        targetFaction: MfFaction?,
        page: Int?
    ) {
        when (val coercionResult = flag.coerce(flagValue)) {
            is MfFlagValueCoercionFailure -> handleCoercionFailure(sender, flag, coercionResult)
            is MfFlagValueCoercionSuccess<*> -> handleCoercionSuccess(sender, faction, flag, coercionResult, targetFaction, page)
        }
    }

    private fun handleCoercionFailure(sender: Player, flag: MfFlag<Any>, coercionResult: MfFlagValueCoercionFailure) {
        sender.sendMessage(
            "$RED${plugin.language[
                "CommandFactionFlagSetValueCoercionFailed",
                flag.type.simpleName ?: plugin.language["UnknownFlagType"]
            ]}: ${coercionResult.failureMessage}"
        )
    }

    private fun handleCoercionSuccess(
        sender: Player,
        faction: MfFaction,
        flag: MfFlag<Any>,
        coercionResult: MfFlagValueCoercionSuccess<*>,
        targetFaction: MfFaction?,
        page: Int?
    ) {
        when (val validationResult = flag.validate(coercionResult.value)) {
            is MfFlagValidationFailure -> handleValidationFailure(sender, validationResult)
            is MfFlagValidationSuccess -> saveFlagAndNotify(sender, faction, flag, coercionResult.value, targetFaction, page)
        }
    }

    private fun handleValidationFailure(sender: Player, validationResult: MfFlagValidationFailure) {
        sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetValueValidationFailed"]}: ${validationResult.failureMessage}")
    }

    private fun saveFlagAndNotify(
        sender: Player,
        faction: MfFaction,
        flag: MfFlag<Any>,
        value: Any,
        targetFaction: MfFaction?,
        page: Int?
    ) {
        val allowNeutrality = plugin.config.getBoolean("factions.allowNeutrality")
        if (flag == plugin.flags.isNeutral && value == true && !allowNeutrality) {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetNeutralityDisabled"]}")
            return
        }

        val factionService = plugin.services.factionService
        factionService.save(faction.copy(flags = faction.flags + (flag to value))).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionFlagSetFailedToSaveFaction"]}")
            plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
            return
        }

        sender.sendMessage("$GREEN${plugin.language["CommandFactionFlagSetSuccess", flag.name, value.toString()]}")
        redirectToFlagList(sender, faction, targetFaction, page)
    }

    private fun redirectToFlagList(sender: Player, faction: MfFaction, targetFaction: MfFaction?, page: Int?) {
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                val factionParam = if (targetFaction != null) " \"${faction.name}\"" else ""
                sender.performCommand("faction flag list$factionParam" + if (page != null) " $page" else "")
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        val hasForcePermission = sender.hasPermission("mf.force.flag")
        val factionService = plugin.services.factionService

        return when {
            args.isEmpty() -> {
                // First arg: could be faction name (if force) or flag name
                if (hasForcePermission) {
                    factionService.factions.map { it.name } + plugin.flags.map { it.name }
                } else {
                    plugin.flags.map { it.name }
                }
            }
            args.size == 1 -> {
                // Filter first argument suggestions
                if (hasForcePermission) {
                    val factionNames = factionService.factions.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
                    val flagNames = plugin.flags.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
                    factionNames + flagNames
                } else {
                    plugin.flags.filter { it.name.lowercase().startsWith(args[0].lowercase()) }.map { it.name }
                }
            }
            args.size == 2 && hasForcePermission -> {
                // Second arg: could be flag name if first arg is faction
                val unquotedArgs = args.unquote()
                val potentialFaction = factionService.getFaction(MfFactionId(unquotedArgs[0])) ?: factionService.getFaction(unquotedArgs[0])
                if (potentialFaction != null) {
                    // First arg is faction, suggest flag names
                    plugin.flags.filter { it.name.lowercase().startsWith(args[1].lowercase()) }.map { it.name }
                } else {
                    // First arg is flag, no more suggestions
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
