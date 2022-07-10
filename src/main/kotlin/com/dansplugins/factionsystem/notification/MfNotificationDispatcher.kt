package com.dansplugins.factionsystem.notification

import com.dansplugins.factionsystem.player.MfPlayer

interface MfNotificationDispatcher {
    fun sendNotification(player: MfPlayer, notification: MfNotification)
}