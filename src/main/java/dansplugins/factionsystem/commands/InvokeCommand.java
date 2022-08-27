/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.misc.ArgumentParser;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class InvokeCommand extends SubCommand {

    public InvokeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{"invoke", LOCALE_PREFIX + "CmdInvoke"}, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final List<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 2) {
            player.sendMessage(ChatColor.RED + "Arguments must be designated in between double quotes.");
            return;
        }
        final Faction invokee = getFaction(argumentsInsideDoubleQuotes.get(0));
        final Faction warringFaction = getFaction(argumentsInsideDoubleQuotes.get(1));
        if (invokee == null || warringFaction == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", argumentsInsideDoubleQuotes.get(0)), true);
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", argumentsInsideDoubleQuotes.get(1)), true);

            return;
        }
        if (!this.faction.isAlly(invokee.getName()) && !this.faction.isVassal(invokee.getName())) {
            playerService.sendMessageType(player, "&c" + getText("NotAnAllyOrVassal", invokee.getName()), Objects.requireNonNull(messageService.getLanguage().getString("NotAnAllyOrVassal")).replace("#name#", invokee.getName()), true);
            return;
        }
        if (!this.faction.isEnemy(warringFaction.getName())) {
            playerService.sendMessageType(player, "&c" + getText("NotAtWarWith", warringFaction.getName())
                    , messageService.getLanguage().getString("NotAtWarWith").replace("#name#", warringFaction.getName())
                    , true);
            return;
        }
        if (configService.getBoolean("allowNeutrality") && ((boolean) invokee.getFlags().getFlag("neutral"))) {
            playerService.sendMessageType(player, "&c" + getText("CannotBringNeutralFactionIntoWar")
                    , "CannotBringNeutralFactionIntoWar", false);
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(invokee, warringFaction, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            invokee.addEnemy(warringFaction.getName());
            warringFaction.addEnemy(invokee.getName());

            messageFaction(invokee, // Message ally faction
                    "&c" + getText("AlertCalledToWar1", faction.getName(), warringFaction.getName()), Objects.requireNonNull(messageService.getLanguage().getString("AlertCalledToWar1")).replace("#f1#", faction.getName()).replace("#f2#", warringFaction.getName()));

            messageFaction(warringFaction, // Message warring faction
                    "&c" + getText("AlertCalledToWar2", faction.getName(), invokee.getName()), Objects.requireNonNull(messageService.getLanguage().getString("AlertCalledToWar2")).replace("#f1#", faction.getName()).replace("#f2#", invokee.getName()));

            messageFaction(this.faction, // Message player faction
                    "&a" + getText("AlertCalledToWar3", invokee.getName(), warringFaction.getName()), Objects.requireNonNull(messageService.getLanguage().getString("AlertCalledToWar3")).replace("#f1#", faction.getName()).replace("#f2#", warringFaction.getName()));
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