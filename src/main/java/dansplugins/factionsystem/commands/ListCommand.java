/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;

/**
 * @author Callum Johnson
 */
public class ListCommand extends SubCommand {

    public ListCommand() {
        super(new String[]{
                "list", LOCALE_PREFIX + "CmdList"
        }, false);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {

    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        final String permission = "mf.list";
        if (!(checkPermissions(sender, permission))) return;
        if (data.getNumFactions() == 0) {
            sender.sendMessage(translate("&b" + getText("CurrentlyNoFactions")));
            return;
        }
        sender.sendMessage(translate("&b&l" + getText("FactionsTitle")));
        List<PersistentData.SortableFaction> sortedFactionList = PersistentData.getInstance().getSortedListOfFactions();
        sender.sendMessage(ChatColor.AQUA + LocalLocaleService.getInstance().getText("ListLegend"));
        sender.sendMessage(ChatColor.AQUA + "-----");
        for (PersistentData.SortableFaction sortableFaction : sortedFactionList) {
            final Faction temp = sortableFaction.getFaction();
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", temp.getName(), "P: " +
                    temp.getCumulativePowerLevel(), "M: " + temp.getPopulation(), "L: " +
                    chunks.getChunksClaimedByFaction(temp.getName())));
        }
    }
}