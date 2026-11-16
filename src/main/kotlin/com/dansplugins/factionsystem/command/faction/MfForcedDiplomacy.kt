package com.dansplugins.factionsystem.command.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.CommandSender
import java.util.logging.Level.SEVERE

/**
 * Forced diplomacy for operators (#1898): `/f declarewar|makepeace|ally|breakalliance|vassalize|grantindependence
 * <faction> <target>` with the matching `mf.force.<command>` node.
 *
 * The forced form acts for both factions at once, so it never waits for the other side's consent or for moderator
 * approval: an alliance or vassalage is formed immediately, and peace is made immediately. The relationship rules that
 * keep the data consistent still apply (no war with an ally or a vassal, no vassal with two lieges, no vassal cycles).
 *
 * The forced form is only taken when the arguments do not already name a target the normal command would accept, so
 * every command that worked before this change behaves exactly as it did.
 */
class MfForcedDiplomacy(private val plugin: MedievalFactions) {

    sealed interface Parse {
        /** The arguments are not a forced form; the command carries on as normal. */
        object NotForced : Parse

        /** The arguments were a forced form, but it was refused. The sender has been told why. */
        object Refused : Parse
        data class Resolved(val faction: MfFaction, val target: MfFaction) : Parse
    }

    /**
     * Reads `<faction> <target>` from [args] for a sender holding the command's force node. Factions with spaces in
     * their name are quoted, or given by ID. Anything after a `--` (the approval reason separator) is ignored.
     */
    fun parse(sender: CommandSender, args: Array<out String>, hasForcePermission: Boolean): Parse {
        if (!hasForcePermission) return Parse.NotForced
        val factionService = plugin.services.factionService
        val beforeReason = args.takeWhile { it != "--" }
        // The normal command treats the whole argument string (or the part before `--`) as the target. When that
        // already names a faction, the command keeps its normal meaning.
        if (factionService.getFaction(args.joinToString(" ")) != null ||
            factionService.getFaction(beforeReason.joinToString(" ")) != null
        ) {
            return Parse.NotForced
        }
        val unquotedArgs = beforeReason.toTypedArray().unquote()
        if (unquotedArgs.size < 2) return Parse.NotForced
        val factionName = unquotedArgs[0]
        val faction = resolveFaction(factionName)
        if (faction == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionForceInvalidFaction", factionName]}")
            return Parse.Refused
        }
        val targetName = unquotedArgs.drop(1).joinToString(" ")
        val target = resolveFaction(targetName)
        if (target == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionForceInvalidFaction", targetName]}")
            return Parse.Refused
        }
        if (faction.id == target.id) {
            sender.sendMessage("$RED${plugin.language["CommandFactionForceSameFaction"]}")
            return Parse.Refused
        }
        return Parse.Resolved(faction, target)
    }

    fun declareWar(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        val (relationships, reverseRelationships) = relationshipsBetween(faction, target)
        if (relationships.any { it.type == AT_WAR } && reverseRelationships.any { it.type == AT_WAR }) {
            return refuse(sender, "CommandFactionForceDeclareWarAlreadyAtWar", faction, target)
        }
        if (relationships.any { it.type == ALLY } && reverseRelationships.any { it.type == ALLY }) {
            return refuse(sender, "CommandFactionForceDeclareWarAllied", faction, target)
        }
        if ((relationships + reverseRelationships).any { it.type == VASSAL }) {
            return refuse(sender, "CommandFactionForceDeclareWarVassal", faction, target)
        }
        if (target.flags[plugin.flags.isNeutral]) {
            return refuse(sender, "CommandFactionForceDeclareWarNeutral", target, faction)
        }
        if (!createMissing(sender, faction, target, AT_WAR, AT_WAR)) return
        succeed(sender, "declared war between", "CommandFactionForceDeclareWarSuccess", faction, target)
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                notify(faction, "FactionAtWarNotification", target)
                notify(target, "FactionAtWarNotification", faction)
                plugin.server.onlinePlayers.filter { onlinePlayer ->
                    (faction.members + target.members).none { member -> member.playerId.value == onlinePlayer.uniqueId.toString() }
                }.forEach { onlinePlayer -> onlinePlayer.sendMessage("$RED${plugin.language["FactionDeclaredWar", faction.name, target.name]}") }
            }
        )
    }

    fun makePeace(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        if (!deleteAll(sender, faction, target, AT_WAR, AT_WAR, "CommandFactionForceMakePeaceNotAtWar")) return
        succeed(sender, "made peace between", "CommandFactionForceMakePeaceSuccess", faction, target)
        notifyLater(faction, "FactionPeaceMadeNotification", target, target, "FactionPeaceMadeNotification", faction)
    }

    fun ally(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        val (relationships, reverseRelationships) = relationshipsBetween(faction, target)
        if (relationships.any { it.type == ALLY } && reverseRelationships.any { it.type == ALLY }) {
            return refuse(sender, "CommandFactionForceAllyAlreadyAllies", faction, target)
        }
        if ((relationships + reverseRelationships).any { it.type == AT_WAR }) {
            return refuse(sender, "CommandFactionForceAllyAtWar", faction, target)
        }
        if (!createMissing(sender, faction, target, ALLY, ALLY)) return
        succeed(sender, "formed an alliance between", "CommandFactionForceAllySuccess", faction, target)
        notifyLater(faction, "FactionAllyNotification", target, target, "FactionAllyNotification", faction)
    }

    fun breakAlliance(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        if (!deleteAll(sender, faction, target, ALLY, ALLY, "CommandFactionForceBreakAllianceNotAllies")) return
        succeed(sender, "broke the alliance between", "CommandFactionForceBreakAllianceSuccess", faction, target)
        notifyLater(faction, "FactionAllianceBrokenNotification", target, target, "FactionAllianceBrokenNotification", faction)
    }

    /** Makes [target] a vassal of [faction]. */
    fun vassalize(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        val relationshipService = plugin.services.factionRelationshipService
        val targetLiegeId = relationshipService.getLiege(target.id)
        if (targetLiegeId == faction.id) {
            return refuse(sender, "CommandFactionForceVassalizeAlreadyVassal", faction, target)
        }
        if (targetLiegeId != null) {
            val targetLiegeName = plugin.services.factionService.getFaction(targetLiegeId)?.name ?: targetLiegeId.value
            sender.sendMessage("$RED${plugin.language["CommandFactionForceVassalizeHasLiege", target.name, targetLiegeName]}")
            return
        }
        if (relationshipService.getVassalTree(target.id).contains(faction.id)) {
            return refuse(sender, "CommandFactionForceVassalizeWouldCreateCycle", faction, target)
        }
        if (!createMissing(sender, faction, target, VASSAL, LIEGE)) return
        succeed(sender, "made a vassal relationship between", "CommandFactionForceVassalizeSuccess", faction, target)
        notifyLater(faction, "FactionNewVassalNotification", target, target, "FactionFealtySwornNotification", faction)
    }

    /** Releases [target] from being a vassal of [faction]. */
    fun grantIndependence(sender: CommandSender, faction: MfFaction, target: MfFaction) {
        if (plugin.services.factionRelationshipService.getLiege(target.id) != faction.id) {
            return refuse(sender, "CommandFactionForceGrantIndependenceNotAVassal", faction, target)
        }
        if (!deleteAll(sender, faction, target, VASSAL, LIEGE, null)) return
        succeed(sender, "granted independence in", "CommandFactionForceGrantIndependenceSuccess", faction, target)
        notifyLater(faction, "VassalGrantedIndependenceNotification", target, target, "FactionGrantedIndependenceNotification", faction)
    }

    private fun resolveFaction(nameOrId: String): MfFaction? {
        val factionService = plugin.services.factionService
        return factionService.getFaction(MfFactionId(nameOrId)) ?: factionService.getFaction(nameOrId)
    }

    private fun relationshipsBetween(faction: MfFaction, target: MfFaction): Pair<List<MfFactionRelationship>, List<MfFactionRelationship>> {
        val relationshipService = plugin.services.factionRelationshipService
        return relationshipService.getRelationships(faction.id, target.id) to relationshipService.getRelationships(target.id, faction.id)
    }

    /** Saves `faction -> target` of [type] and `target -> faction` of [reverseType], skipping any that already exist. */
    private fun createMissing(
        sender: CommandSender,
        faction: MfFaction,
        target: MfFaction,
        type: MfFactionRelationshipType,
        reverseType: MfFactionRelationshipType
    ): Boolean {
        val relationshipService = plugin.services.factionRelationshipService
        val (relationships, reverseRelationships) = relationshipsBetween(faction, target)
        val toSave = listOfNotNull(
            if (relationships.none { it.type == type }) MfFactionRelationship(factionId = faction.id, targetId = target.id, type = type) else null,
            if (reverseRelationships.none { it.type == reverseType }) MfFactionRelationship(factionId = target.id, targetId = faction.id, type = reverseType) else null
        )
        toSave.forEach { relationship ->
            relationshipService.save(relationship).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandFactionForceFailedToSaveRelationship"]}")
                plugin.logger.log(SEVERE, "Failed to save faction relationship: ${it.reason.message}", it.reason.cause)
                return false
            }
        }
        return true
    }

    /**
     * Deletes every `faction -> target` relationship of [type] and `target -> faction` of [reverseType]. When there
     * are none, the sender is told [noneKey] (if given) and nothing changes.
     */
    private fun deleteAll(
        sender: CommandSender,
        faction: MfFaction,
        target: MfFaction,
        type: MfFactionRelationshipType,
        reverseType: MfFactionRelationshipType,
        noneKey: String?
    ): Boolean {
        val relationshipService = plugin.services.factionRelationshipService
        val (relationships, reverseRelationships) = relationshipsBetween(faction, target)
        val toDelete = relationships.filter { it.type == type } + reverseRelationships.filter { it.type == reverseType }
        if (toDelete.isEmpty()) {
            if (noneKey != null) refuse(sender, noneKey, faction, target)
            return false
        }
        toDelete.forEach { relationship ->
            relationshipService.delete(relationship.id).onFailure {
                sender.sendMessage("$RED${plugin.language["CommandFactionForceFailedToSaveRelationship"]}")
                plugin.logger.log(SEVERE, "Failed to delete faction relationship: ${it.reason.message}", it.reason.cause)
                return false
            }
        }
        return true
    }

    private fun refuse(sender: CommandSender, key: String, faction: MfFaction, target: MfFaction) {
        sender.sendMessage("$RED${plugin.language[key, faction.name, target.name]}")
    }

    private fun succeed(sender: CommandSender, action: String, key: String, faction: MfFaction, target: MfFaction) {
        sender.sendMessage("$GREEN${plugin.language[key, faction.name, target.name]}")
        plugin.logger.info("${sender.name} $action ${faction.name} (${faction.id.value}) and ${target.name} (${target.id.value}) with a force permission")
    }

    private fun notify(faction: MfFaction, key: String, other: MfFaction) {
        faction.sendMessage(plugin.language["${key}Title", other.name], plugin.language["${key}Body", other.name])
    }

    private fun notifyLater(faction: MfFaction, key: String, other: MfFaction, target: MfFaction, targetKey: String, targetOther: MfFaction) {
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                notify(faction, key, other)
                notify(target, targetKey, targetOther)
            }
        )
    }
}
