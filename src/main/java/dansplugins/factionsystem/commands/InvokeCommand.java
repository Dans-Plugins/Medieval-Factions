/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.objects.domain.Faction;
import preponderous.ponder.misc.ArgumentParser;

/**
 * @author Callum Johnson
 */
public class InvokeCommand extends SubCommand {

    public InvokeCommand() {
        super(new String[]{
                "invoke", LOCALE_PREFIX + "CmdInvoke"
        }, true, true, false, true);
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
        final String permission = "mf.invoke";
        if (!(checkPermissions(player, permission))) return;
        if (args.length < 2) {
            player.sendMessage(translate("&c" + "Usage: /mf invoke \"ally\" \"enemy\""));
            return;
        }
        ArgumentParser argumentParser = new ArgumentParser();
        final ArrayList<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 2) {
            player.sendMessage(ChatColor.RED + "Arguments must be designated in between double quotes.");
            return;
        }
        final Faction invokee = getFaction(argumentsInsideDoubleQuotes.get(0));
        final Faction warringFaction = getFaction(argumentsInsideDoubleQuotes.get(1));
        if (invokee == null || warringFaction == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!this.faction.isAlly(invokee.getName()) && !this.faction.isVassal(invokee.getName())) {
            player.sendMessage(translate("&c" + getText("NotAnAllyOrVassal", invokee.getName())));
            return;
        }
        if (!this.faction.isEnemy(warringFaction.getName())) {
            player.sendMessage(translate("&c" + getText("NotAtWarWith", warringFaction.getName())));
            return;
        }
        if (MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality") && ((boolean) invokee.getFlags().getFlag("neutral"))) {
            player.sendMessage(translate("&c" + getText("CannotBringNeutralFactionIntoWar")));
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(invokee, warringFaction, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            invokee.addEnemy(warringFaction.getName());
            warringFaction.addEnemy(invokee.getName());

            messageFaction(invokee, // Message ally faction
                    translate("&c" + getText("AlertCalledToWar1", faction.getName(), warringFaction.getName())));

            messageFaction(warringFaction, // Message warring faction
                    translate("&c" + getText("AlertCalledToWar2", faction.getName(), invokee.getName())));

            messageFaction(this.faction, // Message player faction
                    translate("&a" + getText("AlertCalledToWar3", invokee.getName(), warringFaction.getName())));
        }
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

    }
}