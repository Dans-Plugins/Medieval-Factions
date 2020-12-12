package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.domainobjects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddLawCommand {

    public void addLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.addlaw") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (Utilities.isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                Faction playersFaction = Utilities.getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        String newLaw = Utilities.createStringFromFirstArgOnwards(args);

                        playersFaction.addLaw(newLaw);

                        player.sendMessage(ChatColor.GREEN + "Law added!");
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf addlaw (new law)");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be the owner of your faction to use this command!");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command!");
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.addlaw'");
        }

    }
}
