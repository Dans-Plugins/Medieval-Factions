/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils;

import static org.bukkit.Material.LADDER;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author Daniel McCoy Stephenson
 */
public class InteractionAccessChecker {
    private static InteractionAccessChecker instance;

    private InteractionAccessChecker() {

    }

    public static InteractionAccessChecker getInstance() {
        if (instance == null) {
            instance = new InteractionAccessChecker();
        }
        return instance;
    }

    public boolean shouldEventBeCancelled(ClaimedChunk claimedChunk, Player player) {
        if (factionsProtectionsNotEnabled()) {
            return false;
        }

        if (claimedChunk == null) {
            return false;
        }

        if (isPlayerBypassing(player)) {
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return true;
        }

        return !isLandClaimedByPlayersFaction(playersFaction, claimedChunk) && !isOutsiderInteractionAllowed(player, claimedChunk, playersFaction);
    }

    private boolean isLandClaimedByPlayersFaction(Faction faction, ClaimedChunk claimedChunk) {
        return faction.getName().equalsIgnoreCase(claimedChunk.getHolder());
    }

    private boolean factionsProtectionsNotEnabled() {
        return !MedievalFactions.getInstance().getConfig().getBoolean("factionProtectionsEnabled");
    }

    private boolean isPlayerBypassing(Player player) {
        return EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
    }

    public boolean isOutsiderInteractionAllowed(Player player, ClaimedChunk chunk, Faction playersFaction) {
        if (!MedievalFactions.getInstance().getConfig().getBoolean("factionProtectionsEnabled")) {
            return true;
        }

        final Faction chunkHolder = PersistentData.getInstance().getFaction(chunk.getHolder());

        boolean inVassalageTree = PersistentData.getInstance().isPlayerInFactionInVassalageTree(player, chunkHolder);
        boolean isAlly = playersFaction.isAlly(chunk.getHolder());
        boolean allyInteractionAllowed = (boolean) chunkHolder.getFlags().getFlag("alliesCanInteractWithLand");
        boolean vassalageTreeInteractionAllowed = (boolean) chunkHolder.getFlags().getFlag("vassalageTreeCanInteractWithLand");

        Logger.getInstance().log("allyInteractionAllowed: " + allyInteractionAllowed);
        Logger.getInstance().log("vassalageTreeInteractionAllowed: " + vassalageTreeInteractionAllowed);

        boolean allowed = allyInteractionAllowed && isAlly;

        if (vassalageTreeInteractionAllowed && inVassalageTree) {
            allowed = true;
        }

        return allowed;
    }

    public boolean isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(Block blockPlaced, Player player, ClaimedChunk claimedChunk) {
        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            return false;
        }

        if (claimedChunk == null) {
            return false;
        }

        boolean laddersArePlaceableInEnemyTerritory = MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory");
        boolean playerIsTryingToPlaceLadderInEnemyTerritory = blockPlaced.getType() == LADDER && playersFaction.isEnemy(claimedChunk.getHolder());
        return laddersArePlaceableInEnemyTerritory && playerIsTryingToPlaceLadderInEnemyTerritory;
    }
}