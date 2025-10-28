package com.dansplugins.factionsystem.command.faction.help

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.pagination.PaginatedView
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionHelpCommand(
    private val plugin: MedievalFactions,
) : CommandExecutor,
    TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission("mf.help")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionHelpNoPermission"]}")
            return true
        }

        val pageNumber = args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0

        val view =
            PaginatedView.fromChatComponents(
                plugin.language,
                arrayOf(TextComponent(plugin.language["CommandFactionHelpTitle"]).apply { color = SpigotChatColor.AQUA }),
                listOf(
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpAccessors"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpAccessorsAdd"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpAccessorsList"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpAccessorsRemove"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpDuel"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpDuelAccept"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpDuelCancel"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpDuelChallenge"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFaction"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionAlly"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionAutoclaim"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionBonusPower"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionBreakAlliance"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionBypass"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionChat"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionChatHistory"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionCheckClaim"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionClaim"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionClaimFill"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionCreate"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(
                            plugin.language["CommandFactionHelpFactionDeclareIndependence"],
                        ).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionDeclareWar"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionDisband"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionFlag"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionFlagList"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionFlagSet"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionGrantIndependence"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionHelp"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionHome"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionInfo"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionInvite"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionInvoke"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionJoin"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionKick"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionLaw"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionLawAdd"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionLawList"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionLawRemove"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionLeave"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionList"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionMakePeace"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionMap"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionMembers"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionPower"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRelationship"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionRelationshipAdd"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(
                        TextComponent(
                            plugin.language["CommandFactionHelpFactionRelationshipRemove"],
                        ).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionRelationshipView"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRole"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleCreate"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleDelete"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleList"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleRename"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleSet"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionRoleSetDefault"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionRoleSetPermission"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionRoleView"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionSet"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(
                        TextComponent(plugin.language["CommandFactionHelpFactionSetDescription"]).apply { color = SpigotChatColor.GRAY },
                    ),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionSetName"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionSetPrefix"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionSetHome"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionSwearFealty"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionUnclaim"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionUnclaimAll"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionVassalize"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpFactionWho"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpGate"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpGateCancel"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpGateCreate"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpGateRemove"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpLock"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpPower"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpPowerSet"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpUnlock"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpAddMember"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpShowApps"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpApply"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpApproveApp"]).apply { color = SpigotChatColor.GRAY }),
                    arrayOf(TextComponent(plugin.language["CommandFactionHelpDenyApp"]).apply { color = SpigotChatColor.GRAY }),
                ),
            ) { page -> "/faction help ${page + 1}" }
        if (pageNumber !in view.pages.indices) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionHelpInvalidPageNumber"]}")
            return true
        }
        view.sendPage(sender, pageNumber)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ) = emptyList<String>()
}
