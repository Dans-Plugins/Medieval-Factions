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
public class UnlockCommand extends SubCommand {
    private final RelationChecker relationChecker;

    public UnlockCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, RelationChecker relationChecker, PlayerService playerService, MessageService messageService) {
        super(new String[]{
            "unlock", LOCALE_PREFIX + "CmdUnlock"
        }, true, true, ["mf.unlock"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length != 0 && args[0].equalsIgnoreCase("cancel")) {
            this.ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
            this.ephemeralData.getForcefullyUnlockingPlayers().remove(player.getUniqueId()); // just in case the player tries to cancel a forceful unlock without using the force command
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("AlertUnlockingCancelled"),
                "AlertUnlockingCancelled", 
                false
            );
            return;
        }
        if (!this.ephemeralData.getUnlockingPlayers().contains(player.getUniqueId())) {
            this.ephemeralData.getUnlockingPlayers().add(player.getUniqueId());
        }
        this.ephemeralData.getLockingPlayers().remove(player.getUniqueId());

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        this.playerService.sendMessage(
            player, 
            "&a" + this.getText("RightClickUnlock"),
            "RightClickUnlock", 
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