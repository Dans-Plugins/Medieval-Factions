package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class LeaveCommand {

    public boolean leaveFaction(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.leave") || sender.hasPermission("mf.default")) {
                if (isInFaction(player.getUniqueId(), MedievalFactions.getInstance().factions)) {
                    for (int i = 0; i < MedievalFactions.getInstance().factions.size(); i++) {
                        if (MedievalFactions.getInstance().factions.get(i).isMember(player.getUniqueId())) {
                            if (MedievalFactions.getInstance().factions.get(i).isOwner(player.getUniqueId())) {
                                // is faction empty?
                                if (MedievalFactions.getInstance().factions.get(i).getPopulation() == 1) {
                                    // able to leave

                                    if (MedievalFactions.getInstance().factions.get(i).isOfficer(player.getUniqueId())) {
                                        MedievalFactions.getInstance().factions.get(i).removeOfficer(player.getUniqueId());
                                    }

                                    // remove records of alliances/wars associated with this faction
                                    for (Faction faction : MedievalFactions.getInstance().factions) {
                                        if (faction.isAlly(MedievalFactions.getInstance().factions.get(i).getName())) {
                                            faction.removeAlly(MedievalFactions.getInstance().factions.get(i).getName());
                                        }
                                        if (faction.isEnemy(MedievalFactions.getInstance().factions.get(i).getName())) {
                                            faction.removeEnemy(MedievalFactions.getInstance().factions.get(i).getName());
                                        }
                                        if (faction.isVassal(MedievalFactions.getInstance().factions.get(i).getName())) {
                                            faction.removeVassal(MedievalFactions.getInstance().factions.get(i).getName());
                                        }
                                        if (MedievalFactions.getInstance().factions.get(i).isLiege(faction.getName()))
                                        {
                                            MedievalFactions.getInstance().factions.get(i).setLiege("none");
                                        }
                                    }

                                    MedievalFactions.getInstance().playersInFactionChat.remove(player.getUniqueId());

                                    // delete file associated with faction
                                    System.out.println("Attempting to delete file plugins/MedievalFactions/" + MedievalFactions.getInstance().factions.get(i).getName() + ".txt");
                                    try {
                                        File fileToDelete = new File("plugins/MedievalFactions/" + MedievalFactions.getInstance().factions.get(i).getName() + ".txt");
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
                                    removeAllClaimedChunks(MedievalFactions.getInstance().factions.get(i).getName(), MedievalFactions.getInstance().claimedChunks);

                                    MedievalFactions.getInstance().factions.get(i).removeMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), MedievalFactions.getInstance().playerPowerRecords).getPowerLevel());
                                    MedievalFactions.getInstance().factions.remove(i);
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

                                if (MedievalFactions.getInstance().factions.get(i).isOfficer(player.getUniqueId())) {
                                    MedievalFactions.getInstance().factions.get(i).removeOfficer(player.getUniqueId());
                                }

                                if (MedievalFactions.getInstance().playersInFactionChat.contains(player.getUniqueId())) {
                                    MedievalFactions.getInstance().playersInFactionChat.remove(player.getUniqueId());
                                }

                                MedievalFactions.getInstance().factions.get(i).removeMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), MedievalFactions.getInstance().playerPowerRecords).getPowerLevel());
                                player.sendMessage(ChatColor.AQUA + "You left your faction.");
                                try {
                                    sendAllPlayersInFactionMessage(MedievalFactions.getInstance().factions.get(i), ChatColor.GREEN + player.getName() + " has left " + MedievalFactions.getInstance().factions.get(i).getName());
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
