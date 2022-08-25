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
import dansplugins.factionsystem.utils.RelationChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class LockCommand extends SubCommand {
    private final RelationChecker relationChecker;

    public LockCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, RelationChecker relationChecker, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "lock", LOCALE_PREFIX + "CmdLock"
        }, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
        this.relationChecker = relationChecker;
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
        final String permission = "mf.lock";
        if (relationChecker.playerNotInFaction(player)) {
            return;
        }
        if (!checkPermissions(player, permission)) {
            return;
        }
        if (args.length >= 1 && safeEquals(args[0], "cancel")) {
            if (ephemeralData.getLockingPlayers().remove(player.getUniqueId())) { // Remove them
                playerService.sendMessageType(player, "&c" + getText("LockingCancelled"),
                        "LockingCancelled", false);
                return;
            }
        }
        ephemeralData.getLockingPlayers().add(player.getUniqueId());
        ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
        playerService.sendMessageType(player, "&a" + getText("RightClickLock")
                , "RightClickLock", false);
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