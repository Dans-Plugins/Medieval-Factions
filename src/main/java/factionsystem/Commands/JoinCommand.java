package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class JoinCommand extends Command {

    public JoinCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public boolean joinFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length > 1) {

                // creating name from arguments 1 to the last one
                String factionName = createStringFromFirstArgOnwards(args);

                for (Faction faction : main.factions) {
                    if (faction.getName().equalsIgnoreCase(factionName)) {
                        if (faction.isInvited(player.getUniqueId())) {

                            // join if player isn't in a faction already
                            if (!(isInFaction(player.getUniqueId(), main.factions))) {
                                faction.addMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());
                                faction.uninvite(player.getUniqueId());
                                try {
                                    sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + " has joined " + faction.getName());
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
        return false;
    }

}
