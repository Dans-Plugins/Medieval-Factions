package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.UtilitySubsystem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class PlayerDeathEventHandler {

    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = event.getEntity();
        
        // decrease dying player's power
        for (PlayerPowerRecord record : MedievalFactions.getInstance().playerPowerRecords) {
            if (record.getPlayerUUID().equals(player.getUniqueId())) {
                record.decreasePowerByTenPercent();
                if (getPlayersPowerRecord(player.getUniqueId(), MedievalFactions.getInstance().playerPowerRecords).getPowerLevel() > 0) {
                    player.sendMessage(ChatColor.RED + "Your power level has decreased!");
                }
            }
        }

        // if player's cause of death was another player killing them
        if (player.getKiller() != null) {
            Player killer = player.getKiller();

            PlayerPowerRecord record = UtilitySubsystem.getPlayersPowerRecord(killer.getUniqueId(), MedievalFactions.getInstance().playerPowerRecords);
            if (record != null) {
                if (record.increasePowerByTenPercent()){
                    killer.sendMessage(ChatColor.GREEN + "Your power level has increased!");
                }
            }
        }

        // if player is in faction
        if (isInFaction(player.getUniqueId(), MedievalFactions.getInstance().factions)) {

            // if player is in land claimed by their faction
            double[] playerCoords = new double[2];
            playerCoords[0] = player.getLocation().getChunk().getX();
            playerCoords[1] = player.getLocation().getChunk().getZ();

            // check if land is claimed
            if (UtilitySubsystem.isClaimed(player.getLocation().getChunk(), MedievalFactions.getInstance().claimedChunks))
            {
            	ClaimedChunk chunk = UtilitySubsystem.getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(),
            			player.getLocation().getWorld().getName(), MedievalFactions.getInstance().claimedChunks);
                // if holder is player's faction
                if (chunk.getHolder().equalsIgnoreCase(getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getName()) && getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getAutoClaimStatus() == false) {

                    // if not killed by another player
                    if (!(player.getKiller() instanceof Player)) {

                        // player keeps items
                        // event.setKeepInventory(true); // TODO: fix this duplicating items

                    }

                }
            }

        }

    }
}
