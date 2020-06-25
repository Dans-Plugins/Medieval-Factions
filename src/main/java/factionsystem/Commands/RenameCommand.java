package factionsystem.Commands;

import factionsystem.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.UtilityFunctions.*;

public class RenameCommand {

    Main main = null;

    public RenameCommand(Main plugin) {
        main = plugin;
    }

    public void renameFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.rename") || player.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String newName = createStringFromFirstArgOnwards(args);
                    if (isInFaction(player.getName(), main.factions)) {
                        Faction playersFaction = getPlayersFaction(player.getName(), main.factions);
                        if (playersFaction.isOwner(player.getName())) {
                            // change name
                            playersFaction.changeName(newName);
                            player.sendMessage(ChatColor.GREEN + "Faction name changed!");

                            // save faction and faction names
                            playersFaction.save(main.factions);
                            main.saveFactionNames();
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
