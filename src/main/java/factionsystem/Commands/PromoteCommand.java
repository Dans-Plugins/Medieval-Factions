package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;
import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;
import static org.bukkit.Bukkit.getServer;

public class PromoteCommand extends Command {

    public PromoteCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.promote") || sender.hasPermission("mf.default")) {
                if (isInFaction(player.getUniqueId(), main.factions)) {
                    if (args.length > 1) {
                        for (Faction faction : main.factions) {
                            UUID playerUUID = findUUIDBasedOnPlayerName(args[1]);
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
