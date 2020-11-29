package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;
import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class RemoveLawCommand extends Command {

    public RemoveLawCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void removeLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.removelaw") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (isInFaction(player.getUniqueId(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);

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
