package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;
import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;
import static org.bukkit.Bukkit.getServer;

public class PromoteCommand {

    Main main = null;

    public PromoteCommand(Main plugin) {
        main = plugin;
    }

    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getUniqueId(), main.factions)) {
                if (args.length > 1) {
                    for (Faction faction : main.factions) {
                        UUID playerUUID = findUUIDBasedOnPlayerName(args[1]);
                        if (faction.isMember(playerUUID)) {
                            if (faction.isOwner(player.getUniqueId())) {
                                if (faction.isMember(playerUUID)) {
                                    if (faction.isOfficer(playerUUID)) {
                                        player.sendMessage(ChatColor.GREEN + "Player is already an officer!");
                                        return;
                                    }

                                    if(faction.addOfficer(playerUUID)){
                                        player.sendMessage(ChatColor.GREEN + "Player promoted!");

                                        try {
                                            Player target = getServer().getPlayer(args[1]);
                                            target.sendMessage(ChatColor.GREEN + "You have been promoted to officer status in your faction!");
                                        }
                                        catch(Exception ignored) {

                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED +
                                                "Player can't be promoted because you have reached your limit! Limit: "
                                                + faction.calculateMaxOfficers());
                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That player isn't a member of your faction!");
                                }

                                return;
                            }
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf promote (player-name)");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
