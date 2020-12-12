package factionsystem.Commands;

import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Data.PersistentData;
import factionsystem.Subsystems.StorageSubsystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class RenameCommand {

    public void renameFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.rename") || player.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String oldName = getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName();
                    String newName = createStringFromFirstArgOnwards(args);

                    // existence check
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(newName)) {
                            player.sendMessage(ChatColor.RED + "That name is already taken!");
                            return;
                        }
                    }

                    if (isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                        Faction playersFaction = getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
                        if (playersFaction.isOwner(player.getUniqueId())) {

                            // change name
                            playersFaction.setName(newName);
                            player.sendMessage(ChatColor.GREEN + "Faction name changed!");

                            // rename alliance, enemy, liege and vassal records
                            for (Faction faction : PersistentData.getInstance().getFactions()) {
                                if (faction.isAlly(oldName)) {
                                    faction.removeAlly(oldName);
                                    faction.addAlly(newName);
                                }
                                if (faction.isEnemy(oldName)) {
                                    faction.removeEnemy(oldName);
                                    faction.addEnemy(newName);
                                }
                                if (faction.isLiege(oldName)) {
                                    faction.setLiege(newName);
                                }
                                if (faction.isVassal(oldName)) {
                                    faction.removeVassal(oldName);
                                    faction.addVassal(newName);
                                }
                            }

                            // rename claimed chunk records
                            for (ClaimedChunk claimedChunk : PersistentData.getInstance().getClaimedChunks()) {
                                if (claimedChunk.getHolder().equalsIgnoreCase(oldName)) {
                                    claimedChunk.setHolder(newName);
                                }
                            }

                            // rename locked block records
                            for (LockedBlock lockedBlock : PersistentData.getInstance().getLockedBlocks()) {
                                if (lockedBlock.getFactionName().equalsIgnoreCase(oldName)) {
                                    lockedBlock.setFaction(newName);
                                }
                            }

                            // Save again to overwrite current data
                            StorageSubsystem.getInstance().save();

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You are not the owner of this faction!");
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf rename (newName)");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! You need to have the permission 'mf.rename' to use this command.");
            }
        }
    }
}
