package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.LockedBlock;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RenameCommand {

    public void renameFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.rename")) {
                if (args.length > 1) {
                    String oldName = PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName();
                    String newName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    // existence check
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(newName)) {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NameAlreadyTaken"));
                            return;
                        }
                    }

                    if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                        if (playersFaction.isOwner(player.getUniqueId())) {

                            // change name
                            playersFaction.setName(newName);
                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("FactionNameChanged"));

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
                            StorageManager.getInstance().save();

                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotTheOwnerOfThisFaction"));
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageRename"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.rename"));
            }
        }
    }
}
