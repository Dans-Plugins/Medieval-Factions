package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class PromoteCommand {

    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.promote") || sender.hasPermission("mf.default")) {
                if (Utilities.getInstance().isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID playerUUID = Utilities.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.isMember(playerUUID)) {
                                        if (faction.isOfficer(playerUUID)) {
                                            player.sendMessage(ChatColor.GREEN + "Player is already an officer!");
                                            return;
                                        }

                                        if(faction.addOfficer(playerUUID)){
                                            player.sendMessage(ChatColor.GREEN + "Player promoted!");

                                            try {
                                                Player target = getServer().getPlayer(args[1]);
                                                target.sendMessage(ChatColor.RED + "You have been promoted to officer status in your faction!");
                                            }
                                            catch(Exception ignored) {

                                            }
                                        } else {
                                            player.sendMessage(ChatColor.RED +
                                                    "Player can't be promoted because you have reached your limit! Limit: "
                                                    + faction.calculateMaxOfficers());
                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "That player isn't a member of your faction!");
                                    }

                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf promote (player-name)");
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.promote'");
            }
        }
    }
}
