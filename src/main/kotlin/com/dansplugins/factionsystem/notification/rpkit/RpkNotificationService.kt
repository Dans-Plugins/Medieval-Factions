package com.dansplugins.factionsystem.notification.rpkit

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.notification.MfNotification
import com.dansplugins.factionsystem.notification.MfNotificationService
import com.dansplugins.factionsystem.player.MfPlayerId
import com.rpkit.core.service.Services
import com.rpkit.notifications.bukkit.notification.RPKNotificationService
import com.rpkit.players.bukkit.profile.RPKProfile
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfileService

class RpkNotificationService(private val plugin: MedievalFactions) : MfNotificationService {
    override fun sendNotification(playerId: MfPlayerId, notification: MfNotification) {
        val minecraftProfileService = Services.INSTANCE.get(RPKMinecraftProfileService::class.java) ?: return
        val bukkitPlayer = playerId.toBukkitPlayer()
        val minecraftProfile = minecraftProfileService.getMinecraftProfile(bukkitPlayer).join() ?: return
        val profile = minecraftProfile.profile
        if (profile !is RPKProfile) return
        val notificationService = Services.INSTANCE.get(RPKNotificationService::class.java) ?: return
        notificationService.createNotification(profile, notification.title, notification.body).join()
    }
}
