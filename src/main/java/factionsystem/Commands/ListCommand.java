package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

import static factionsystem.Subsystems.UtilitySubsystem.getChunksClaimedByFaction;

public class ListCommand {

    Main main = null;

    public ListCommand(Main plugin) {
        main = plugin;
    }

    public boolean listFactions(CommandSender sender) {
        // if there aren't any factions
        if (main.factions.size() == 0) {
            sender.sendMessage(ChatColor.AQUA + "There are currently no factions.");
        }
        // factions exist, list them
        else {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + " == Factions" + " == ");
            listFactionsWithFormatting(sender);
        }
        return true;
    }

    public void listFactionsWithFormatting(CommandSender sender) {
        sender.sendMessage(String.format("%-10s %-5s %-12s %-4s", "Name", "Power", "Population", "Land"));
        for (Faction faction : main.utilities.getFactionsSortedByPower()) {
            sender.sendMessage(String.format("%-10s %-5d %-12d %-4d", faction.getName(), faction.getCumulativePowerLevel(), faction.getPopulation(), getChunksClaimedByFaction(faction.getName(), main.claimedChunks)));
        }
    }
}
