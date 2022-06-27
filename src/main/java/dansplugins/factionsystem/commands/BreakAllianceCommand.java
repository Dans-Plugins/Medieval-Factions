/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author Callum Johnson
 */
public class BreakAllianceCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public BreakAllianceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "breakalliance", "ba", LOCALE_PREFIX + "CmdBreakAlliance"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
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
        final String permission = "mf.breakalliance";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageBreakAlliance")));
            return;
        }

        final Faction otherFaction = getFaction(String.join(" ", args));
        if (otherFaction == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        if (otherFaction == faction) {
            player.sendMessage(translate("&c" + getText("CannotBreakAllianceWithSelf")));
            return;
        }

        if (!faction.isAlly(otherFaction.getName())) {
            player.sendMessage(translate("&c" + getText("AlertNotAllied", otherFaction.getName())));
            return;
        }

        faction.removeAlly(otherFaction.getName());
        otherFaction.removeAlly(faction.getName());
        messageFaction(faction, translate("&c" + getText("AllianceBrokenWith", otherFaction.getName())));
        messageFaction(otherFaction, translate("&c" + getText("AlertAllianceHasBeenBroken", faction.getName())));
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