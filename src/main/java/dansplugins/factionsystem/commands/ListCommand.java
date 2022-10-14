/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Callum Johnson
 */
public class ListCommand extends SubCommand {

    public ListCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "list", LOCALE_PREFIX + "CmdList"
        }, false, ["mf.list"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (this.persistentData.getNumFactions() == 0) {
            this.playerService.sendMessage(
                sender, 
                "&b" + this.getText("CurrentlyNoFactions"),
                "CurrentlyNoFactions", 
                false
            );
            return;
        }
        this.playerService.sendMessage(
            sender, 
            "&b&l" + this.getText("FactionsTitle"),
            "FactionsTitle", 
            false
        );
        List<PersistentData.SortableFaction> sortedFactionList = this.persistentData.getSortedListOfFactions();
        sender.sendMessage(ChatColor.AQUA + this.localeService.get("ListLegend"));
        sender.sendMessage(ChatColor.AQUA + "-----");
        for (PersistentData.SortableFaction sortableFaction : sortedFactionList) {
            final Faction temp = sortableFaction.getFaction();
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", temp.getName(), "P: " +
                    temp.getCumulativePowerLevel(), "M: " + temp.getPopulation(), "L: " +
                    this.chunkDataAccessor.getChunksClaimedByFaction(temp.getName())));
        }
    }
}