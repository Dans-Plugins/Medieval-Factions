package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand {

    Main main = null;

    public UnlockCommand(Main plugin) {
        main = plugin;
    }

    public void unlockBlock(CommandSender sender, String[] args) {
        // check if player
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // check if has permission
            if (player.hasPermission("mf.unlock") || player.hasPermission("mf.default")) {

                // check if argument exists
                if (args.length > 1) {

                    // cancel unlock status if first argument is "cancel"
                    if (args[1].equalsIgnoreCase("cancel")) {
                        if (main.unlockingPlayers.contains(player.getName())) {
                            main.unlockingPlayers.remove(player.getName());
                            player.sendMessage(ChatColor.RED + "Unlocking cancelled!");
                            return;
                        }
                    }

                    // check that player has not already invoked this command without unlocking something
                    if (!main.unlockingPlayers.contains(player.getName())) {
                        // add player to playersAboutToLockSomething list
                        main.unlockingPlayers.add(player.getName());

                        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
                        player.sendMessage(ChatColor.GREEN + "Right click a chest or door to unlock it! (Type /mf unlock cancel to cancel)");
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.unlock'");
            }

        }
    }

}
