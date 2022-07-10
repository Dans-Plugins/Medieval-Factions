package com.dansplugins.factionsystem.notification

import com.dansplugins.factionsystem.player.MfPlayer

class NoOpNotificationDispatcher : MfNotificationDispatcher {
    override fun sendNotification(player: MfPlayer, notification: MfNotification) {}
}