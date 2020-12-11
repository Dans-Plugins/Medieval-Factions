package factionsystem.Commands;

import factionsystem.EphemeralData;
import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class KickCommand {

    public boolean kickPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.kick") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    boolean owner = false;
                    for (Faction faction : MedievalFactions.getInstance().factions) {
                        if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                            owner = true;
                            UUID playerUUID = findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (!(args[1].equalsIgnoreCase(player.getName()))) {
                                    if (!(playerUUID.equals(faction.getOwner()))) {

                                        if (faction.isOfficer(playerUUID)) {
                                            faction.removeOfficer(playerUUID);
                                        }

                                        EphemeralData.getInstance().getPlayersInFactionChat().remove(playerUUID);

                                        faction.removeMember(playerUUID, getPlayersPowerRecord(player.getUniqueId(), MedievalFactions.getInstance().playerPowerRecords).getPowerLevel());
                                        try {
                                            sendAllPlayersInFactionMessage(faction, ChatColor.RED + args[1] + " has been kicked from " + faction.getName());
                                        } catch (Exception ignored) {

                                        }
                                        try {
                                            Player target = Bukkit.getServer().getPlayer(args[1]);
                                            target.sendMessage(ChatColor.RED + "You have been kicked from your faction by " + player.getName() + ".");
                                        } catch (Exception ignored) {

                                        }
                                        return true;
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "You can't kick the owner.");
                                        return false;
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You can't kick yourself.");
                                    return false;
                                }

                            }
                        }
                    }

                    if (!owner) {
                        player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                        return false;
                    }

                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf kick (player-name)");
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.kick'");
                return false;
            }
        }
        return false;
    }

}
