package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class VersionCommand extends SubCommand {

    public VersionCommand() {
        super(new String[] {
                "version", LOCALE_PREFIX + "CmdVersion"
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
        final String permission = "mf.version";
        if (!(checkPermissions(sender, permission))) return;
        sender.sendMessage(translate("&bMedieval-Factions-" + MedievalFactions.getInstance().getVersion()));
    }
}