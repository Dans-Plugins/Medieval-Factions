package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand {

    MedievalFactions main = null;

    public UnlockCommand(MedievalFactions plugin) {
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
                        if (main.unlockingPlayers.contains(player.getUniqueId())) {
                            main.unlockingPlayers.remove(player.getUniqueId());
                            player.sendMessage(ChatColor.RED + "Unlocking cancelled!");
                            return;
                        }
                    }
                }

                // check that player has not already invoked this command without unlocking something
                if (!main.unlockingPlayers.contains(player.getUniqueId())) {
                    // add player to playersAboutToLockSomething list
                    main.unlockingPlayers.add(player.getUniqueId());

                    main.lockingPlayers.remove(player.getUniqueId());

                    // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
                    player.sendMessage(ChatColor.GREEN + "Right click a chest or door to unlock it! (Type /mf unlock cancel to cancel)");
                }



            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.unlock'");
            }

        }
    }

}
