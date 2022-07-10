package com.dansplugins.factionsystem.notification

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dansplugins.mailboxes.Mailboxes
import java.util.*

class MailboxesNotificationDispatcher(private val plugin: MedievalFactions) : MfNotificationDispatcher {
    override fun sendNotification(player: MfPlayer, notification: MfNotification) {
        val mailboxesPlugin: Mailboxes = plugin.server.pluginManager.getPlugin("Mailboxes") as? Mailboxes ?: return
        val mailboxesApi = mailboxesPlugin.api
        mailboxesApi.sendPluginMessageToPlayer(plugin.name, player.id.value.let(UUID::fromString), "${notification.title} - ${notification.body}")
    }
}