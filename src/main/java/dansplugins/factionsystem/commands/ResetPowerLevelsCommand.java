package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class ResetPowerLevelsCommand extends SubCommand {

    public ResetPowerLevelsCommand() {
        super(new String[] {
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
        final int initialPowerLevel = getConfig().getInt("initialPowerLevel");
        System.out.println(getText("ResettingIndividualPowerRecords"));
        data.getPlayerPowerRecords().forEach(record -> record.setPowerLevel(initialPowerLevel));
    }
}