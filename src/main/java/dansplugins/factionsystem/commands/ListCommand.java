package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class ListCommand {

    public boolean listFactions(CommandSender sender) {

        if (sender.hasPermission("mf.list") || sender.hasPermission("mf.default")) {
            // if there aren't any factions
            if (PersistentData.getInstance().getFactions().size() == 0) {
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
        for (Faction faction : getFactionsSortedByPower()) {
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", faction.getName(), "P: " + faction.getCumulativePowerLevel(), "M: " + faction.getPopulation(), "L: " + ChunkManager.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks())));
        }
    }

    private ArrayList<Faction> getFactionsSortedByPower() {
        ArrayList<Faction> copiedList = new ArrayList<>(PersistentData.getInstance().getFactions());
        ArrayList<Faction> sortedList = new ArrayList<>();
        while (copiedList.size() != 0) {
            int mostPower = 0;
            int counter = 0;
            int nextMostPowerfulFaction = 0;
            for (Faction faction : copiedList) {
                if (faction.getCumulativePowerLevel() > mostPower) {
                    mostPower = faction.getCumulativePowerLevel();
                    nextMostPowerfulFaction = counter;
                }
                counter++;
            }
            sortedList.add(copiedList.get(nextMostPowerfulFaction));
            copiedList.remove(nextMostPowerfulFaction);
        }
        return sortedList;
    }
}
