package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static org.bukkit.Bukkit.getServer;

public class DemoteCommand {

    Main main = null;

    public DemoteCommand(Main plugin) {
        main = plugin;
    }

    public void demotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getUniqueId(), main.factions)) {
                if (args.length > 1) {
                    for (Faction faction : main.factions) {
                        UUID officerUUID = findUUIDBasedOnPlayerName(args[1]);
                        if (faction.isOfficer(officerUUID)) {
                            if (faction.isOwner(player.getUniqueId())) {
                                if (faction.removeOfficer(officerUUID) == true) {

                                    player.sendMessage(ChatColor.GREEN + "Player demoted!");

                                    try {
                                        Player target = getServer().getPlayer(officerUUID);
                                        target.sendMessage(ChatColor.RED + "You have been demoted to member status in your faction.");
                                    }
                                    catch(Exception ignored) {

                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That player isn't an officer in your faction!");
                                }
                                return;
                            }
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf demote (player-name)");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
