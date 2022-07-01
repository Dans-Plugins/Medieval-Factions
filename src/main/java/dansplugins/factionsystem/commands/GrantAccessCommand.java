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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class GrantAccessCommand extends SubCommand {

    public GrantAccessCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "grantaccess", "ga", LOCALE_PREFIX + "CmdGrantAccess"
        }, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantAccess")));
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            player.sendMessage(translate("&c" + getText("CommandCancelled")));
            return;
        }
        if (ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyGrantingAccess")));
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            player.sendMessage(translate("&c" + getText("CannotGrantAccessToSelf")));
            return;
        }
        ephemeralData.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        player.sendMessage(translate("&a" + getText("RightClickGrantAccess", args[0])));
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