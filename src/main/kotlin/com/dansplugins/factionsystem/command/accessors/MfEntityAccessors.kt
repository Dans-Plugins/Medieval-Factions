package com.dansplugins.factionsystem.command.accessors

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.locks.MfLockedEntity
import com.dansplugins.factionsystem.pagination.PaginatedView
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor

/**
 * The `/accessors ... entity <uuid>` forms, which manage the access list of a locked armour stand or
 * item frame (#1679). Right-clicking the entity in an accessor mode runs these, as clicking a block
 * runs the coordinate forms. The rules are the block rules: the owner and the players already on the
 * list may add and remove players.
 */
class MfEntityAccessors(private val plugin: MedievalFactions) {

    /** Parses the entity UUID argument, telling [sender] the usage when it is missing or malformed. */
    fun parseEntityId(sender: Player, args: List<String>, usageKey: String): UUID? {
        val entityId = args.firstOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (entityId == null) sender.sendMessage("$RED${plugin.language[usageKey]}")
        return entityId
    }

    fun add(sender: Player, entityId: UUID, accessor: OfflinePlayer) =
        edit(sender, entityId, accessor, "CommandAccessorsAdd") { lockedEntity, accessorId ->
            lockedEntity.copy(accessors = (lockedEntity.accessors + accessorId).distinctBy { it.value })
        }

    fun remove(sender: Player, entityId: UUID, accessor: OfflinePlayer) =
        edit(sender, entityId, accessor, "CommandAccessorsRemove") { lockedEntity, accessorId ->
            lockedEntity.copy(accessors = lockedEntity.accessors.filter { it.value != accessorId.value })
        }

    private fun edit(
        sender: Player,
        entityId: UUID,
        accessor: OfflinePlayer,
        keyPrefix: String,
        change: (MfLockedEntity, MfPlayerId) -> MfLockedEntity
    ) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["${keyPrefix}FailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val accessorMfPlayer = playerService.getPlayer(accessor)
                    ?: playerService.save(MfPlayer(plugin, accessor)).onFailure {
                        sender.sendMessage("$RED${plugin.language["${keyPrefix}FailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val lockService = plugin.services.lockService
                val lockedEntity = lockService.getLockedEntity(entityId)
                if (lockedEntity == null) {
                    sender.sendMessage("$RED${plugin.language["CommandAccessorsEntityNotLocked"]}")
                    return@Runnable
                }
                if (!lockedEntity.hasAccess(mfPlayer.id)) {
                    sender.sendMessage("$RED${plugin.language["${keyPrefix}MustHaveAccess"]}")
                    return@Runnable
                }
                lockService.saveLockedEntity(change(lockedEntity, accessorMfPlayer.id)).onFailure {
                    sender.sendMessage("$RED${plugin.language["${keyPrefix}FailedToSaveLockedBlock"]}")
                    plugin.logger.log(SEVERE, "Failed to save locked entity: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["${keyPrefix}Success", accessor.name ?: plugin.language["UnknownPlayer"]]}")
                plugin.server.scheduler.runTask(plugin, Runnable { sender.performCommand("accessors list entity $entityId") })
            }
        )
    }

    fun list(sender: Player, entityId: UUID, pageNumber: Int) {
        val lockedEntity = plugin.services.lockService.getLockedEntity(entityId)
        if (lockedEntity == null) {
            sender.sendMessage("$RED${plugin.language["CommandAccessorsEntityNotLocked"]}")
            return
        }
        val playerService = plugin.services.playerService
        fun nameOf(playerId: MfPlayerId): TextComponent {
            val name = playerService.getPlayer(playerId)?.toBukkit()?.name
            return TextComponent(name ?: plugin.language["UnknownPlayer"]).apply {
                color = if (name != null) SpigotChatColor.GRAY else SpigotChatColor.RED
            }
        }
        val view = PaginatedView(
            plugin.language,
            lazy {
                arrayOf(
                    TextComponent(plugin.language["CommandAccessorsListTitle"]).apply {
                        color = SpigotChatColor.AQUA
                        isBold = true
                    }
                )
            },
            lockedEntity.accessors.map { accessor ->
                lazy {
                    arrayOf(
                        TextComponent("✖ ").apply {
                            color = SpigotChatColor.RED
                            clickEvent = ClickEvent(RUN_COMMAND, "/accessors remove entity $entityId ${accessor.value}")
                            hoverEvent = HoverEvent(
                                SHOW_TEXT,
                                Text(plugin.language["CommandAccessorsListDeleteAccessorButtonHover", playerService.getPlayer(accessor)?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]])
                            )
                        },
                        nameOf(accessor)
                    )
                }
            } + lazy { arrayOf(nameOf(lockedEntity.playerId)) }
        ) { page -> "/accessors list entity $entityId ${page + 1}" }
        if (pageNumber !in view.pages.indices) {
            sender.sendMessage("$RED${plugin.language["CommandAccessorsListInvalidPageNumber"]}")
            return
        }
        view.sendPage(sender, pageNumber)
        if (sender.hasPermission("mf.accessors.add")) {
            sender.spigot().sendMessage(
                TextComponent(plugin.language["CommandAccessorsListAddAccessor"]).apply {
                    color = SpigotChatColor.GREEN
                    clickEvent = ClickEvent(RUN_COMMAND, "/accessors add entity $entityId")
                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandAccessorsListAddAccessorHover"]))
                }
            )
        }
    }
}
