package com.dansplugins.factionsystem.notification.rpkit;

import com.dansplugins.factionsystem.notification.MfNotification;
import com.dansplugins.factionsystem.notification.MfNotificationService;
import com.dansplugins.factionsystem.player.MfPlayer;
import com.rpkit.core.service.Services;
import com.rpkit.notifications.bukkit.notification.RPKNotificationService;
import com.rpkit.players.bukkit.profile.RPKProfile;
import com.rpkit.players.bukkit.profile.RPKThinProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfile;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfileService;
import org.jetbrains.annotations.NotNull;

public final class RpkNotificationService implements MfNotificationService {

    @Override
    public void sendNotification(@NotNull MfPlayer player, @NotNull MfNotification notification) {
        RPKMinecraftProfileService minecraftProfileService = Services.INSTANCE.get(RPKMinecraftProfileService.class);
        if (minecraftProfileService == null) return;
        RPKMinecraftProfile minecraftProfile = minecraftProfileService.getMinecraftProfile(player.toBukkit()).join();
        if (minecraftProfile == null) return;
        RPKThinProfile thinProfile = minecraftProfile.getProfile();
        if (!(thinProfile instanceof RPKProfile profile)) return;
        RPKNotificationService notificationService = Services.INSTANCE.get(RPKNotificationService.class);
        if (notificationService == null) return;
        notificationService.createNotification(profile, notification.getTitle(), notification.getBody()).join();
    }

}
