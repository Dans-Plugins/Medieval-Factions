package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class LeaveCommand {

    MedievalFactions main = null;

    public LeaveCommand(MedievalFactions plugin) {
        main = plugin;
    }

    public boolean leaveFaction(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getUniqueId(), main.factions)) {
                for (int i = 0; i < main.factions.size(); i++) {
                    if (main.factions.get(i).isMember(player.getUniqueId())) {
                        if (main.factions.get(i).isOwner(player.getUniqueId())) {
                            // is faction empty?
                            if (main.factions.get(i).getPopulation() == 1) {
                                // able to leave

                                if (main.factions.get(i).isOfficer(player.getUniqueId())) {
                                    main.factions.get(i).removeOfficer(player.getUniqueId());
                                }

                                // remove records of alliances/wars associated with this faction
                                for (Faction faction : main.factions) {
                                    if (faction.isAlly(main.factions.get(i).getName())) {
                                        faction.removeAlly(main.factions.get(i).getName());
                                    }
                                    if (faction.isEnemy(main.factions.get(i).getName())) {
                                        faction.removeEnemy(main.factions.get(i).getName());
                                    }
                                    if (faction.isVassal(main.factions.get(i).getName())) {
                                    	faction.removeVassal(main.factions.get(i).getName());
                                    }
                                    if (main.factions.get(i).isLiege(faction.getName()))
                                    {
                                    	main.factions.get(i).setLiege("none");
                                    }
                                }

                                main.playersInFactionChat.remove(player.getUniqueId());

                                // delete file associated with faction
                                System.out.println("Attempting to delete file plugins/MedievalFactions/" + main.factions.get(i).getName() + ".txt");
                                try {
                                    File fileToDelete = new File("plugins/MedievalFactions/" + main.factions.get(i).getName() + ".txt");
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
                                removeAllClaimedChunks(main.factions.get(i).getName(), main.claimedChunks);

                                main.factions.get(i).removeMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());
                                main.factions.remove(i);
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

                            if (main.factions.get(i).isOfficer(player.getUniqueId())) {
                                main.factions.get(i).removeOfficer(player.getUniqueId());
                            }

                            if (main.playersInFactionChat.contains(player.getUniqueId())) {
                                main.playersInFactionChat.remove(player.getUniqueId());
                            }

                            main.factions.get(i).removeMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());
                            player.sendMessage(ChatColor.AQUA + "You left your faction.");
                            try {
                                sendAllPlayersInFactionMessage(main.factions.get(i), ChatColor.GREEN + player.getName() + " has left " + main.factions.get(i).getName());
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
        return false;
    }
}
