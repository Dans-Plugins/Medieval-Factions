package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class WhoCommand {

    Main main = null;

    public WhoCommand(Main plugin) {
        main = plugin;
    }

    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                String name = createStringFromFirstArgOnwards(args);
                Faction faction = getPlayersFaction(findUUIDBasedOnPlayerName(name), main.factions);
                if (faction != null) {
                    sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), main.claimedChunks));
                }
                else {
                    player.sendMessage(ChatColor.RED + "That player isn't in a faction.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf who (player-name)");
            }

        }
    }

}
