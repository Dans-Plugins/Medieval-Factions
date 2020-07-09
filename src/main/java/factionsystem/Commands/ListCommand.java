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
        // create list of faction names
        ArrayList<String> factionNames = new ArrayList<>();
        for (Faction faction : main.factions) {
            factionNames.add(faction.getName());
        }
        int longestNameLength = main.utilities.getLongestStringLength(factionNames);

        String headers = "";
        headers = headers + "Name";
        for (int i = 0; i < longestNameLength - 4; i++) {
            headers = headers + " ";
        }
        headers = headers + " Power Population Land";

        sender.sendMessage(ChatColor.AQUA + headers);
        for (Faction faction : main.utilities.getFactionsSortedByPower()) {
            sender.sendMessage(ChatColor.AQUA + "" + faction.getName() + " " + faction.getCumulativePowerLevel() + "      " + faction.getPopulation() + "           " + getChunksClaimedByFaction(faction.getName(), main.claimedChunks));
        }
    }
}
