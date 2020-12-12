package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.domainobjects.Faction;
import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DescCommand {

    public boolean setDescription(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.desc") || sender.hasPermission("mf.default")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isOwner(player.getUniqueId())) {
                        owner = true;
                        if (args.length > 1) {

                            // set arg[1] - args[args.length-1] to be the description with spaces put in between
                            String newDesc = "";
                            for (int i = 1; i < args.length; i++) {
                                newDesc = newDesc + args[i];
                                if (!(i == args.length - 1)) {
                                    newDesc = newDesc + " ";
                                }
                            }

                            faction.setDescription(newDesc);
                            player.sendMessage(ChatColor.AQUA + "Description set!");
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf desc (description)");
                            return false;
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.desc'");
                return false;
            }
        }
        return false;
    }

}
