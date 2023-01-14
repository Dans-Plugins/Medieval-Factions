package com.dansplugins.factionsystem.notification

import com.dansplugins.factionsystem.player.MfPlayerId

interface MfNotificationService {
    fun sendNotification(playerId: MfPlayerId, notification: MfNotification)
}
