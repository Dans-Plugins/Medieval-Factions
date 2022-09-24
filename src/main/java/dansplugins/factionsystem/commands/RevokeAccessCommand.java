/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
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
public class RevokeAccessCommand extends SubCommand {

    public RevokeAccessCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "ra", "revokeaccess", LOCALE_PREFIX + "CmdRevokeAccess"
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
        final String permission = "mf.revokeaccess";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessage(player, "&c" + getText("UsageRevokeAccess")
                    , "UsageRevokeAccess", false);
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());
            playerService.sendMessage(player, "&c" + getText("Cancelled"), "Cancelled", false);
            return;
        }
        if (ephemeralData.getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
            playerService.sendMessage(player, "&c" + getText("AlreadyEnteredRevokeAccess")
                    , "AlreadyEnteredRevokeAccess", false);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            playerService.sendMessage(player, "&c" + getText("CannotRevokeAccessFromSelf")
                    , "CannotRevokeAccessFromSelf", false);
            return;
        }
        ephemeralData.getPlayersRevokingAccess().put(
                player.getUniqueId(), targetUUID
        );
        playerService.sendMessage(player, "&a" + getText("RightClickRevokeAccess")
                , "RightClickRevokeAccess", false);
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