package com.dansplugins.factionsystem.notification

import com.dansplugins.factionsystem.player.MfPlayer

interface MfNotificationService {
    fun sendNotification(player: MfPlayer, notification: MfNotification)
}