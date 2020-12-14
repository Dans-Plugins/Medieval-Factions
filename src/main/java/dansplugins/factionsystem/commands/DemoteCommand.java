package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class DemoteCommand {

    public void demotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.demote") || sender.hasPermission("mf.default")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID officerUUID = Utilities.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (officerUUID != null && faction.isOfficer(officerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.removeOfficer(officerUUID)) {

                                        player.sendMessage(ChatColor.GREEN + "Player demoted!");

                                        try {
                                            Player target = getServer().getPlayer(officerUUID);
                                            target.sendMessage(ChatColor.RED + "You have been demoted to member status in your faction.");
                                        }
                                        catch(Exception ignored) {

                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "That player isn't an officer in your faction!");
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf demote (player-name)");
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.demote'");
            }
        }
    }
}
