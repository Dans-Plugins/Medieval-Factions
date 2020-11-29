package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;
import static org.bukkit.Bukkit.getServer;

public class TransferCommand extends Command {

    public TransferCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public boolean transferOwnership(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (Faction faction : main.factions) {
                if (faction.isOwner(player.getUniqueId())) {
                    owner = true;
                    if (args.length > 1) {
                        UUID playerUUID = findUUIDBasedOnPlayerName(args[1]);
                        if (faction.isMember(playerUUID)) {

                            if (playerUUID.equals(player.getUniqueId())) {
                                player.sendMessage(ChatColor.RED + "You can't transfer ownership of your faction to yourself!");
                                return false;
                            }

                            if (faction.isOfficer(playerUUID)) {
                                faction.removeOfficer(playerUUID);
                            }

                            // set owner
                            faction.setOwner(playerUUID);
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
