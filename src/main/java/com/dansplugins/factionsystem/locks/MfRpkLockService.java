package com.dansplugins.factionsystem.locks;

import com.dansplugins.factionsystem.MedievalFactions;
import com.dansplugins.factionsystem.area.MfBlockPosition;
import com.dansplugins.factionsystem.interaction.MfInteractionService;
import com.dansplugins.factionsystem.interaction.MfInteractionStatus;
import com.rpkit.core.bukkit.location.LocationsKt;
import com.rpkit.core.location.RPKBlockLocation;
import com.rpkit.core.service.Services;
import com.rpkit.locks.bukkit.lock.RPKLockService;
import com.rpkit.players.bukkit.profile.minecraft.RPKMinecraftProfile;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

import static com.dansplugins.factionsystem.interaction.MfInteractionStatus.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

public final class MfRpkLockService implements RPKLockService {

    private final MedievalFactions plugin;

    public MfRpkLockService(MedievalFactions plugin) {
        this.plugin = plugin;
        Services.INSTANCE.set(RPKLockService.class, this);
    }

    @Override
    public ItemStack getLockItem() {
        return null;
    }

    @Override
    public boolean isLocked(RPKBlockLocation location) {
        MfLockService lockService = plugin.services.getLockService();
        return lockService.getLockedBlock(MfBlockPosition.Companion.fromBukkitBlock(LocationsKt.toBukkitBlock(location))) != null;
    }

    @Override
    public CompletableFuture<Void> setLocked(RPKBlockLocation location, boolean isLocked) {
        return completedFuture(null);
    }

    @Override
    public boolean isClaiming(RPKMinecraftProfile minecraftProfile) {
        return plugin.services.getInteractionService().getInteractionStatus(minecraftProfile.getMinecraftUUID().toString()) == LOCKING;
    }

    @Override
    public boolean isUnclaiming(RPKMinecraftProfile minecraftProfile) {
        return plugin.services.getInteractionService().getInteractionStatus(minecraftProfile.getMinecraftUUID().toString()) == UNLOCKING;
    }

    @Override
    public CompletableFuture<Void> setUnclaiming(RPKMinecraftProfile minecraftProfile, boolean isUnclaiming) {
        return runAsync(() -> {
            MfInteractionService interactionService = plugin.services.getInteractionService();
            MfInteractionStatus currentStatus = interactionService.getInteractionStatus(minecraftProfile.getMinecraftUUID().toString());
            interactionService.setInteractionStatus(
                    minecraftProfile.getMinecraftUUID().toString(),
                    isUnclaiming ? UNLOCKING : (currentStatus == UNLOCKING ? null : currentStatus)
            );
        });
    }

    @Override
    public boolean isGettingKey(RPKMinecraftProfile minecraftProfile) {
        return plugin.services.getInteractionService().getInteractionStatus(minecraftProfile.getMinecraftUUID().toString()) == ADDING_ACCESSOR;
    }

    @Override
    public CompletableFuture<Void> setGettingKey(RPKMinecraftProfile minecraftProfile, boolean isGettingKey) {
        return runAsync(() -> {
            MfInteractionService interactionService = plugin.services.getInteractionService();
            MfInteractionStatus currentStatus = interactionService.getInteractionStatus(minecraftProfile.getMinecraftUUID().toString());
            interactionService.setInteractionStatus(
                    minecraftProfile.getMinecraftUUID().toString(),
                    isGettingKey ? ADDING_ACCESSOR : (currentStatus == ADDING_ACCESSOR ? null : currentStatus)
            );
        });
    }

    @Override
    public ItemStack getKeyFor(RPKBlockLocation location) {
        return null;
    }

    @Override
    public boolean isKey(ItemStack itemStack) {
        return false;
    }

    @Override
    public MedievalFactions getPlugin() {
        return plugin;
    }
}
