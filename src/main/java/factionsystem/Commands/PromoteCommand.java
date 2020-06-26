package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Utility.UtilityFunctions.isInFaction;
import static org.bukkit.Bukkit.getServer;

public class PromoteCommand {

    Main main = null;

    public PromoteCommand(Main plugin) {
        main = plugin;
    }

    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getName(), main.factions)) {
                if (args.length > 1) {
                    for (Faction faction : main.factions) {
                        if (faction.isMember(args[1])) {
                            if (faction.isOwner(player.getName())) {
                                if (faction.isMember(args[1])) {
                                    faction.addOfficer(args[1]);
                                    player.sendMessage(ChatColor.GREEN + "Player promoted!");

                                    try {
                                        Player target = getServer().getPlayer(args[1]);
                                        target.sendMessage(ChatColor.GREEN + "You have been promoted to officer status in your faction!");
                                    }
                                    catch(Exception ignored) {

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
