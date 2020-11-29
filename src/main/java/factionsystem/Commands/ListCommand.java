package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static factionsystem.Subsystems.UtilitySubsystem.getChunksClaimedByFaction;

public class ListCommand extends Command {

    public ListCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public boolean listFactions(CommandSender sender) {

        if (sender.hasPermission("mf.list") || sender.hasPermission("mf.default")) {
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
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.list'");
            return false;
        }
    }

    private void listFactionsWithFormatting(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "P: power, M: members, L: land");
        sender.sendMessage(ChatColor.AQUA + "-----");
        for (Faction faction : main.utilities.getFactionsSortedByPower()) {
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", faction.getName(), "P: " + faction.getCumulativePowerLevel(), "M: " + faction.getPopulation(), "L: " + getChunksClaimedByFaction(faction.getName(), main.claimedChunks)));
        }
    }
}
