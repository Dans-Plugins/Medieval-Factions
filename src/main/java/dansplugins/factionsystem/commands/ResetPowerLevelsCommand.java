/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;

/**
 * @author Callum Johnson
 */
public class ResetPowerLevelsCommand extends SubCommand {

    public ResetPowerLevelsCommand() {
        super(new String[]{
                "resetpowerlevels", LOCALE_PREFIX + "CmdResetPowerLevels", "rpl"
        }, false);
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
        if (!(checkPermissions(sender, "mf.resetpowerlevels", "mf.admin"))) return;
        sender.sendMessage(translate("&aPower Levels Resetting..."));
        System.out.println(getText("ResettingIndividualPowerRecords"));
        PersistentData.getInstance().resetPowerLevels();
    }
}