package com.dansplugins.factionsystem.notification.noop

import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.notification.MfNotificationService
import com.dansplugins.factionsystem.player.MfPlayerId

class NoOpNotificationService : MfNotificationService {
    override fun sendNotification(playerId: MfPlayerId, notification: MfNotification) {}
}
