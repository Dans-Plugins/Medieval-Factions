package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.domainobjects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveLawCommand {

    public void removeLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.removelaw") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (Utilities.isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                Faction playersFaction = Utilities.getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        int lawToRemove = Integer.parseInt(args[1]) - 1;

                        if (playersFaction.removeLaw(lawToRemove)) {
                            player.sendMessage(ChatColor.GREEN + "Law " + (lawToRemove + 1) + " removed!");
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf removelaw (number)");
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command!");
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.removelaw'");
        }

    }
}
