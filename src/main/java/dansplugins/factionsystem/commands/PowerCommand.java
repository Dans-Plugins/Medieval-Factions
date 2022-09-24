/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class PowerCommand extends SubCommand {

    public PowerCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "power", LOCALE_PREFIX + "CmdPower"
        }, false, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.power";
        if (!(checkPermissions(sender, permission))) return;
        final PowerRecord record;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                playerService.sendMessage(sender, getText("OnlyPlayersCanUseCommand")
                        , "OnlyPlayersCanUseCommand", false);
                return;
            }
            record = persistentData.getPlayersPowerRecord(((Player) sender).getUniqueId());
            playerService.sendMessage(sender, "&b" +
                            getText("AlertCurrentPowerLevel", record.getPower(), record.maxPower())
                    , Objects.requireNonNull(messageService.getLanguage().getString("AlertCurrentPowerLevel"))
                            .replace("#power#", String.valueOf(record.getPower()))
                            .replace("#max#", String.valueOf(record.maxPower())), true);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID target = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (target == null) {
            playerService.sendMessage(sender, "&c" + getText("PlayerNotFound"),
                    Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
            return;
        }
        record = persistentData.getPlayersPowerRecord(target);
        playerService.sendMessage(sender, "&b" +
                        getText("CurrentPowerLevel", args[0], record.getPower(), record.maxPower())
                , Objects.requireNonNull(messageService.getLanguage().getString("CurrentPowerLevel"))
                        .replace("#power#", String.valueOf(record.getPower()))
                        .replace("#max#", String.valueOf(record.maxPower()))
                        .replace("#name#", args[0]), true);
    }
}