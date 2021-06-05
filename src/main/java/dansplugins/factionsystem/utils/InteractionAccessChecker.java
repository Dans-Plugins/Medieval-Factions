package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static org.bukkit.Material.LADDER;

public class InteractionAccessChecker {

    final private boolean debug = true;

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
        if (claimedChunk == null) {
            // chunk is not claimed
            return false;
        }

        boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
        if (isPlayerBypassing) {
            // player is bypassing
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            // player is not in a faction
            return true;
        }

        boolean isLandClaimedByPlayersFaction = playersFaction.getName().equalsIgnoreCase(claimedChunk.getHolder());
        if (!isLandClaimedByPlayersFaction && !isOutsiderInteractionAllowed(player, claimedChunk, playersFaction)) {
            // land is not claimed by players faction and outsider interaction is disallowed
            return true;
        }
        else {
            // land is claimed by players faction or outsider interaction is allowed
            return false;
        }
    }

    public boolean isOutsiderInteractionAllowed(Player player, ClaimedChunk chunk, Faction playersFaction) {
        final Faction chunkHolder = PersistentData.getInstance().getFaction(chunk.getHolder());

        boolean inVassalageTree = PersistentData.getInstance().isPlayerInFactionInVassalageTree(player, chunkHolder);
        boolean isAlly = playersFaction.isAlly(chunk.getHolder());
        boolean allyInteractionAllowed = (boolean) chunkHolder.getFlags().getFlag("alliesCanInteractWithLand");
        boolean vassalageTreeInteractionAllowed = (boolean) chunkHolder.getFlags().getFlag("vassalageTreeCanInteractWithLand");

        if (debug) {
            System.out.println("[DEBUG] allyInteractionAllowed: " + allyInteractionAllowed);
            System.out.println("[DEBUG] vassalageTreeInteractionAllowed: " + vassalageTreeInteractionAllowed);
        }

        boolean allowed = false;

        if (allyInteractionAllowed && isAlly) {
            allowed = true;
        }

        if (vassalageTreeInteractionAllowed && inVassalageTree) {
            allowed = true;
        }

        return allowed;
    }

    public boolean isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(Block blockPlaced, Player player, ClaimedChunk claimedChunk) {
        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            // player is not in a faction, so they couldn't be trying to place anything in enemy territory
            return false;
        }

        if (claimedChunk == null) {
            // chunk is not claimed, so they couldn't be trying to place anything in enemy territory
            return false;
        }

        boolean laddersArePlaceableInEnemyTerritory = MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory");
        boolean playerIsTryingToPlaceLadderInEnemyTerritory = blockPlaced.getType() == LADDER && playersFaction.isEnemy(claimedChunk.getHolder());
        return laddersArePlaceableInEnemyTerritory && playerIsTryingToPlaceLadderInEnemyTerritory;
    }

}
