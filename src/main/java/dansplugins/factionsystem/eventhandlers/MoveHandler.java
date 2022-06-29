/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import static org.bukkit.Bukkit.getServer;

import java.util.Objects;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author Daniel McCoy Stephenson
 */
public class MoveHandler implements Listener {
    private final PersistentData persistentData;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final DynmapIntegrator dynmapIntegrator;

    public MoveHandler(PersistentData persistentData, TerritoryOwnerNotifier territoryOwnerNotifier, LocaleService localeService, MedievalFactions medievalFactions, DynmapIntegrator dynmapIntegrator) {
        this.persistentData = persistentData;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.dynmapIntegrator = dynmapIntegrator;
    }

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        if (playerEnteredANewChunk(event)) {
            Player player = event.getPlayer();

            initiateAutoclaimCheck(player);

            if (newChunkIsClaimedAndOldChunkWasNot(event)) {
                String factionName = persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = persistentData.getFaction(factionName);
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            if (newChunkIsUnclaimedAndOldChunkWasNot(event)) {
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
                return;
            }

            if (newChunkIsClaimedAndOldChunkWasAlsoClaimed(event) && chunkHoldersAreNotEqual(event)) {
                String factionName = persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder();
                Faction holder = persistentData.getFaction(factionName);
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
            }

        }
    }

    /**
     * This event handler method will deal with liquid moving from one block to another.
     */
    @EventHandler()
    public void handle(BlockFromToEvent event) {
        ClaimedChunk fromChunk = persistentData.getChunkDataAccessor().getClaimedChunk(event.getBlock().getChunk());
        ClaimedChunk toChunk = persistentData.getChunkDataAccessor().getClaimedChunk(event.getToBlock().getChunk());

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
        Faction playersFaction = persistentData.getPlayersFaction(player.getUniqueId());
        if (playersFaction != null && playersFaction.isOwner(player.getUniqueId())) {
            if (playersFaction.getAutoClaimStatus()) {
                if (notAtDemesneLimit(playersFaction)) {
                    scheduleClaiming(player, playersFaction);
                } else {
                    player.sendMessage(ChatColor.RED + localeService.get("AlertReachedDemesne"));
                }
            }
        }
    }

    private boolean notAtDemesneLimit(Faction faction) {
        return persistentData.getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) < faction.getCumulativePowerLevel();
    }

    private void scheduleClaiming(Player player, Faction faction) {
        getServer().getScheduler().runTaskLater(medievalFactions, () -> {
            // add new chunk to claimed chunks
            persistentData.getChunkDataAccessor().claimChunkAtLocation(player, player.getLocation(), faction);
            dynmapIntegrator.updateClaims();
        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
    }

    private boolean newChunkIsClaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && !persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsUnclaimedAndOldChunkWasNot(PlayerMoveEvent event) {
        return !persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean newChunkIsClaimedAndOldChunkWasAlsoClaimed(PlayerMoveEvent event) {
        return persistentData.getChunkDataAccessor().isClaimed(Objects.requireNonNull(event.getTo()).getChunk()) && persistentData.getChunkDataAccessor().isClaimed(event.getFrom().getChunk());
    }

    private boolean chunkHoldersAreNotEqual(PlayerMoveEvent event) {
        return !(persistentData.getChunkDataAccessor().getClaimedChunk(event.getFrom().getChunk()).getHolder().equalsIgnoreCase(persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getTo()).getChunk()).getHolder()));
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