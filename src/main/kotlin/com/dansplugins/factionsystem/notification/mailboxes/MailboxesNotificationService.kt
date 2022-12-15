package com.dansplugins.factionsystem.notification.mailboxes

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.notification.MfNotificationService
import com.dansplugins.factionsystem.player.MfPlayerId
import dansplugins.mailboxes.Mailboxes
import java.util.*

class MailboxesNotificationService(private val plugin: MedievalFactions) : MfNotificationService {
    override fun sendNotification(playerId: MfPlayerId, notification: MfNotification) {
        val mailboxesPlugin: Mailboxes = plugin.server.pluginManager.getPlugin("Mailboxes") as? Mailboxes ?: return
        val mailboxesApi = mailboxesPlugin.api
        mailboxesApi.sendPluginMessageToPlayer(plugin.name, playerId.value.let(UUID::fromString), "${notification.title} - ${notification.body}")
    }
}
