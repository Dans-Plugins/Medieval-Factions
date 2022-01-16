/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class MoveHandler implements Listener {

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        if (playerEnteredANewChunk(event)) {
            Player player =  event.getPlayer();

            initiateAutoclaimCheck(player);

            if (newChunkIsClaimedAndOldChunkWasNot(event)) {
                String factionName = LocalChunkService.getInstance().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = PersistentData.getInstance().getFaction(factionName);
                TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, holder);
                return;
            }

            if (newChunkIsUnclaimedAndOldChunkWasNot(event)) {
                TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, null);
                return;
            }

            if (newChunkIsClaimedAndOldChunkWasAlsoClaimed(event) && chunkHoldersAreNotEqual(event)) {
                String factionName = LocalChunkService.getInstance().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
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
        ClaimedChunk fromChunk = LocalChunkService.getInstance().getClaimedChunk(event.getBlock().getChunk());
        ClaimedChunk toChunk = LocalChunkService.getInstance().getClaimedChunk(event.getToBlock().getChunk());

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
                }
                else {
                    player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertReachedDemesne"));
                }
            }
        }
    }

    private boolean notAtDemesneLimit(Faction faction) {
        return LocalChunkService.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()) < faction.getCumulativePowerLevel();
    }

    private void scheduleClaiming(Player player, Faction faction) {
        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), () -> {
            // add new chunk to claimed chunks
            LocalChunkService.getInstance().claimChunkAtLocation(player, player.getLocation(), faction);
            DynmapIntegrator.getInstance().updateClaims();
        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
    }

    private boolean newChunkIsClaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return LocalChunkService.getInstance().isClaimed(Objects.requireNonNull(event.getTo()).getChunk(), PersistentData.getInstance().getClaimedChunks()) && !LocalChunkService.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks());
    }

    private boolean newChunkIsUnclaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return !LocalChunkService.getInstance().isClaimed(Objects.requireNonNull(event.getTo()).getChunk(), PersistentData.getInstance().getClaimedChunks()) && LocalChunkService.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks());
    }

    private boolean newChunkIsClaimedAndOldChunkWasAlsoClaimed(PlayerMoveEvent event) {
        return LocalChunkService.getInstance().isClaimed(Objects.requireNonNull(event.getTo()).getChunk(), PersistentData.getInstance().getClaimedChunks()) && LocalChunkService.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks());
    }

    private boolean chunkHoldersAreNotEqual(PlayerMoveEvent event) {
        return !(LocalChunkService.getInstance().getClaimedChunk(event.getFrom().getChunk()).getHolder().equalsIgnoreCase(LocalChunkService.getInstance().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder()));
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