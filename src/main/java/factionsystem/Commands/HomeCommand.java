package factionsystem.Commands;

import factionsystem.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.getPlayersFaction;
import static factionsystem.UtilityFunctions.isInFaction;
import static org.bukkit.Bukkit.getServer;

public class HomeCommand {

    Main main = null;

    public HomeCommand(Main plugin) {
        main = plugin;
    }

    public void teleportPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), main.factions);
                if (playersFaction.getFactionHome() != null) {
                    player.sendMessage(ChatColor.GREEN + "Teleporting in 3 seconds...");
                    int seconds = 3;

                    Location initialLocation = player.getLocation();

                    getServer().getScheduler().runTaskLater(main, new Runnable() {
                        @Override
                        public void run() {
                            if (initialLocation.distance(player.getLocation()) == 0) {
                                player.teleport(playersFaction.getFactionHome());
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "Movement Detected. Teleport cancelled.");
                            }

                        }
                    }, seconds * 20);

                }
                else {
                    player.sendMessage(ChatColor.RED + "The faction home isn't set yet.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
