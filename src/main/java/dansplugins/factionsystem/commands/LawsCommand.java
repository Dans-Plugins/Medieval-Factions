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
import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
public class LawsCommand extends SubCommand {

    public LawsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "laws", LOCALE_PREFIX + "CmdLaws"
        }, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.laws";
        if (!(checkPermissions(player, permission))) return;
        final Faction target;
        if (args.length == 0) {
            target = getPlayerFaction(player);
            if (target == null) {
                playerService.sendMessage(player, "&c" + getText("AlertMustBeInFactionToUseCommand")
                        , "AlertMustBeInFactionToUseCommand", false);
                return;
            }
            if (target.getNumLaws() == 0) {
                playerService.sendMessage(player, "&c" + getText("AlertNoLaws")
                        , "AlertNoLaws", false);
                return;
            }
        } else {
            target = getFaction(String.join(" ", args));
            if (target == null) {
                playerService.sendMessage(player, "&c" + getText("FactionNotFound"),
                        Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                                .replace("#faction#", String.join(" ", args)), true);
                return;
            }
            if (target.getNumLaws() == 0) {
                playerService.sendMessage(player, "&c" + getText("FactionDoesNotHaveLaws")
                        , "FactionDoesNotHaveLaws", false);
                return;
            }
        }
        playerService.sendMessage(player, "&b" + getText("LawsTitle", target.getName())
                , Objects.requireNonNull(messageService.getLanguage().getString("LawsTitle"))
                        .replace("#name#", target.getName()), true);
        IntStream.range(0, target.getNumLaws())
                .mapToObj(i -> translate("&b" + (i + 1) + ". " + target.getLaws().get(i)))
                .forEach(player::sendMessage);
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