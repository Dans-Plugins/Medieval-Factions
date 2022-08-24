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
            new PlayerService().sendMessageType(player, "&c" + getText("UsageGrantAccess"),
                    "UsageGrantAccess", false);
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            new PlayerService().sendMessageType(player, "&c" + getText("CommandCancelled"), "CommandCancelled", false);
            return;
        }
        if (ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            new PlayerService().sendMessageType(player, "&c" + getText("AlertAlreadyGrantingAccess")
                    , "AlertAlreadyGrantingAccess", false);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            new PlayerService().sendMessageType(player, "&c" + getText("PlayerNotFound")
                    , Objects.requireNonNull(new MessageService().getLanguage().getString("PlayerNotFound")).replaceAll("#name#", args[0]), true);
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            new PlayerService().sendMessageType(player, "&c" + getText("CannotGrantAccessToSelf")
                    , "CannotGrantAccessToSelf", false);
            return;
        }
        ephemeralData.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        new PlayerService().sendMessageType(player,"&a" + getText("RightClickGrantAccess", args[0])
        , Objects.requireNonNull(new MessageService().getLanguage().getString("RightClickGrantAccess")).replaceAll("#name#", args[0]), true);
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