package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand {

    public boolean joinFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.join") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {

                    // creating name from arguments 1 to the last one
                    String factionName = Utilities.getInstance().createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(factionName)) {
                            if (faction.isInvited(player.getUniqueId())) {

                                // join if player isn't in a faction already
                                if (!(PersistentData.getInstance().isInFaction(player.getUniqueId()))) {
                                    faction.addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                    faction.uninvite(player.getUniqueId());
                                    try {
                                        Utilities.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + " has joined " + faction.getName());
                                    } catch (Exception ignored) {

                                    }
                                    player.sendMessage(ChatColor.GREEN + "You joined the faction!");
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You're already in a faction, sorry!");
                                    return false;
                                }

                            } else {
                                player.sendMessage(ChatColor.RED + "You're not invited to this faction!");
                                return false;
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf join (faction-name)");
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.join'");
                return false;
            }
        }
        return false;
    }

}
