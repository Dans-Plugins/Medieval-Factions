package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class LeaveCommand {

    public boolean leaveFaction(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.leave") || sender.hasPermission("mf.default")) {
                if (Utilities.getInstance().isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                    for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                        if (PersistentData.getInstance().getFactions().get(i).isMember(player.getUniqueId())) {
                            if (PersistentData.getInstance().getFactions().get(i).isOwner(player.getUniqueId())) {
                                // is faction empty?
                                if (PersistentData.getInstance().getFactions().get(i).getPopulation() == 1) {
                                    // able to leave

                                    if (PersistentData.getInstance().getFactions().get(i).isOfficer(player.getUniqueId())) {
                                        PersistentData.getInstance().getFactions().get(i).removeOfficer(player.getUniqueId());
                                    }

                                    // remove records of alliances/wars associated with this faction
                                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                                        if (faction.isAlly(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeAlly(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (faction.isEnemy(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeEnemy(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (faction.isVassal(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeVassal(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (PersistentData.getInstance().getFactions().get(i).isLiege(faction.getName()))
                                        {
                                            PersistentData.getInstance().getFactions().get(i).setLiege("none");
                                        }
                                    }

                                    EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());

                                    // delete file associated with faction
                                    System.out.println("Attempting to delete file plugins/MedievalFactions/" + PersistentData.getInstance().getFactions().get(i).getName() + ".txt");
                                    try {
                                        File fileToDelete = new File("plugins/MedievalFactions/" + PersistentData.getInstance().getFactions().get(i).getName() + ".txt");
                                        if (fileToDelete.delete()) {
                                            System.out.println("Success. File deleted.");
                                        }
                                        else {
                                            System.out.println("There was a problem deleting the file.");
                                        }
                                    } catch(Exception e) {
                                        System.out.println("An error has occurred during file deletion.");
                                    }

                                    // remove claimed land objects associated with this faction
                                    ChunkManager.getInstance().removeAllClaimedChunks(PersistentData.getInstance().getFactions().get(i).getName(), PersistentData.getInstance().getClaimedChunks());

                                    PersistentData.getInstance().getFactions().get(i).removeMember(player.getUniqueId(), Utilities.getInstance().getPlayersPowerRecord(player.getUniqueId(), PersistentData.getInstance().getPlayerPowerRecords()).getPowerLevel());
                                    PersistentData.getInstance().getFactions().remove(i);
                                    player.sendMessage(ChatColor.AQUA + "You left your faction. It was deleted since no one else was a member.");

                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "Sorry! You must transfer ownership or kick everyone in your faction to leave.");
                                    return false;
                                }
                            }
                            else {
                                // able to leave

                                if (PersistentData.getInstance().getFactions().get(i).isOfficer(player.getUniqueId())) {
                                    PersistentData.getInstance().getFactions().get(i).removeOfficer(player.getUniqueId());
                                }

                                if (EphemeralData.getInstance().getPlayersInFactionChat().contains(player.getUniqueId())) {
                                    EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
                                }

                                PersistentData.getInstance().getFactions().get(i).removeMember(player.getUniqueId(), Utilities.getInstance().getPlayersPowerRecord(player.getUniqueId(), PersistentData.getInstance().getPlayerPowerRecords()).getPowerLevel());
                                player.sendMessage(ChatColor.AQUA + "You left your faction.");
                                try {
                                    Utilities.getInstance().sendAllPlayersInFactionMessage(PersistentData.getInstance().getFactions().get(i), ChatColor.GREEN + player.getName() + " has left " + PersistentData.getInstance().getFactions().get(i).getName());
                                } catch (Exception ignored) {

                                }
                                return true;
                            }
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.leave'");
                return false;
            }
        }
        return false;
    }
}
