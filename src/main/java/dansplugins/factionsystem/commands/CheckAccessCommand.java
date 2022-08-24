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
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class CheckAccessCommand extends SubCommand {

    public CheckAccessCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "ca", "checkaccess", LOCALE_PREFIX + "CmdCheckAccess"
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
        final String permission = "mf.checkaccess";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        boolean cancel = false, contains = ephemeralData.getPlayersCheckingAccess().contains(player.getUniqueId());

        if (args.length >= 1) {
            cancel = args[0].equalsIgnoreCase("cancel");
        }

        if (cancel && contains) {
            ephemeralData.getPlayersCheckingAccess().remove(player.getUniqueId());
            new PlayerService().sendMessageType(player, "&c" + getText("Cancelled"), "Cancelled", false);
        } else {
            if (contains) {
                new PlayerService().sendMessageType(player, "&c" + getText("AlreadyEnteredCheckAccess"), "AlreadyEnteredCheckAccess", false);
            } else {
                ephemeralData.getPlayersCheckingAccess().add(player.getUniqueId());
                new PlayerService().sendMessageType(player, "&a" + getText("RightClickCheckAccess"), "RightClickCheckAccess", false);
            }
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