/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import static org.bukkit.Bukkit.getServer;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;

/**
 * @author Daniel McCoy Stephenson
 */
public class MoveHandler implements Listener {

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        if (playerEnteredANewChunk(event)) {
            Player player = event.getPlayer();

            initiateAutoclaimCheck(player);

            if (newChunkIsClaimedAndOldChunkWasNot(event)) {
                String factionName = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = PersistentData.getInstance().getFaction(factionName);
                TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, holder);
                return;
            }

            if (newChunkIsUnclaimedAndOldChunkWasNot(event)) {
                TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, null);
                return;
            }

            if (newChunkIsClaimedAndOldChunkWasAlsoClaimed(event) && chunkHoldersAreNotEqual(event)) {
                String factionName = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = PersistentData.getInstance().getFaction(factionName);
                TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, holder);
            }

        }
    }

    /**
     * This event handler method will deal with liquid moving from one block to another.
     */
    @EventHandler()
    public void handle(BlockFromToEvent event) {
        ClaimedChunk fromChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(event.getBlock().getChunk());
        ClaimedChunk toChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(event.getToBlock().getChunk());

        if (playerMovedFromUnclaimedLandIntoClaimedLand(fromChunk, toChunk)) {
            event.setCancelled(true);
            return;
        }

        if (playerMovedFromClaimedLandIntoClaimedLand(fromChunk, toChunk) && holdersOfChunksAreDifferent(fromChunk, toChunk)) {
            event.setCancelled(true);
        }
    }

    private boolean playerEnteredANewChunk(PlayerMoveEvent event) {
        return event.getFrom().getChunk() != Objects.requireNonNull(event.getTo()).getChunk();
    }

    private void initiateAutoclaimCheck(Player player) {
        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (playersFaction != null && playersFaction.isOwner(player.getUniqueId())) {
            if (playersFaction.getAutoClaimStatus()) {
                if (notAtDemesneLimit(playersFaction)) {
                    scheduleClaiming(player, playersFaction);
                } else {
                    player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertReachedDemesne"));
                }
            }
        }
    }

    private boolean notAtDemesneLimit(Faction faction) {
        return PersistentData.getInstance().getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) < faction.getCumulativePowerLevel();
    }

    private void scheduleClaiming(Player player, Faction faction) {
        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), () -> {
            // add new chunk to claimed chunks
            PersistentData.getInstance().getChunkDataAccessor().claimChunkAtLocation(player, player.getLocation(), faction);
            DynmapIntegrator.getInstance().updateClaims();
        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
    }

    private boolean newChunkIsClaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return PersistentData.getInstance().getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && !PersistentData.getInstance().getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsUnclaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return !PersistentData.getInstance().getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && PersistentData.getInstance().getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsClaimedAndOldChunkWasAlsoClaimed(PlayerMoveEvent event) {
        return PersistentData.getInstance().getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && PersistentData.getInstance().getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean chunkHoldersAreNotEqual(PlayerMoveEvent event) {
        return !(PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(event.getFrom().getChunk()).getHolder().equalsIgnoreCase(PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder()));
    }

    private boolean playerMovedFromUnclaimedLandIntoClaimedLand(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return fromChunk == null && toChunk != null;
    }

    private boolean holdersOfChunksAreDifferent(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return !fromChunk.getHolder().equalsIgnoreCase(toChunk.getHolder());
    }

    private boolean playerMovedFromClaimedLandIntoClaimedLand(ClaimedChunk fromChunk, ClaimedChunk toChunk) {
        return fromChunk != null && toChunk != null;
    }

}