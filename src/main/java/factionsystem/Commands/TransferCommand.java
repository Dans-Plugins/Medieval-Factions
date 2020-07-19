package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getServer;

public class TransferCommand {

    Main main = null;

    public TransferCommand(Main plugin) {
        main = plugin;
    }

    public boolean transferOwnership(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (Faction faction : main.factions) {
                if (faction.isOwner(player.getName())) {
                    owner = true;
                    if (args.length > 1) {
                        if (faction.isMember(args[1])) {

                            if (faction.isOfficer(args[1])) {
                                faction.removeOfficer(args[1]);
                            }

                            // set owner
                            faction.setOwner(args[1]);
                            player.sendMessage(ChatColor.AQUA + "Ownership transferred to " + args[1]);

                            try {
                                Player target = getServer().getPlayer(args[1]);
                                target.sendMessage(ChatColor.GREEN + "Ownership of " + faction.getName() + " has been transferred to you.");
                            }
                            catch(Exception ignored) {

                            }


                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "That player isn't in your faction!");
                            return false;
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf transfer (player-name)");
                        return false;
                    }
                }
            }
            if (!owner) {
                player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                return false;
            }
        }
        return false;
    }
}
