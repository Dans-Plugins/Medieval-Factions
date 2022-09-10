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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class SwearFealtyCommand extends SubCommand {

    public SwearFealtyCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "swearfealty", LOCALE_PREFIX + "CmdSwearFealty", "sf"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final String permission = "mf.swearfealty";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsageSwearFealty")
                    , "UsageSwearFealty", false);
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true);
            return;
        }
        if (!target.hasBeenOfferedVassalization(faction.getName())) {
            playerService.sendMessageType(player, "&c" + getText("AlertNotOfferedVassalizationBy")
                    , "AlertNotOfferedVassalizationBy", false);
            return;
        }
        // set vassal
        target.addVassal(faction.getName());
        target.removeAttemptedVassalization(faction.getName());

        // set liege
        faction.setLiege(target.getName());

        // inform target faction that they have a new vassal
        messageFaction(target, translate("&a" + getText("AlertFactionHasNewVassal", faction.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AlertFactionHasNewVassal"))
                        .replace("#name#", faction.getName()));

        // inform players faction that they have a new liege
        messageFaction(faction, translate("&a" + getText("AlertFactionHasBeenVassalized", target.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AlertFactionHasBeenVassalized"))
                        .replace("#name#", target.getName()));
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