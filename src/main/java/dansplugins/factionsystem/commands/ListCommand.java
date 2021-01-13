package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class ListCommand {

    public boolean listFactions(CommandSender sender) {

        if (sender.hasPermission("mf.list")) {
            // if there aren't any factions
            if (PersistentData.getInstance().getFactions().size() == 0) {
                sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("CurrentlyNoFactions"));
            }
            // factions exist, list them
            else {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("FactionsTitle"));
                listFactionsWithFormatting(sender);
            }
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.list"));
            return false;
        }
    }

    private void listFactionsWithFormatting(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("ListLegend"));
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
