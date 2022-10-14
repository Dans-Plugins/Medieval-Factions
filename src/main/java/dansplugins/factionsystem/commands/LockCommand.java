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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class LockCommand extends SubCommand {
    public LockCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, RelationChecker relationChecker, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "lock", LOCALE_PREFIX + "CmdLock"
        }, true, true, ["mf.lock"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length >= 1 && this.safeEquals(args[0], "cancel")) {
            if (this.ephemeralData.getLockingPlayers().remove(player.getUniqueId())) { // Remove them
                this.playerService.sendMessage(
                    player, 
                    "&c" + this.getText("LockingCancelled"),
                    "LockingCancelled", 
                    false
                );
                return;
            }
        }
        this.ephemeralData.getLockingPlayers().add(player.getUniqueId());
        this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
        this.playerService.sendMessage(
            player, 
            "&a" + this.getText("RightClickLock"),
            "RightClickLock", 
            false
        );
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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        return TabCompleteTools.completeSingleOption(args[0], "cancel");
    }
}