package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class FlagsCommand extends SubCommand {

    public FlagsCommand() {
        super(new String[]{
                "flags", LOCALE_PREFIX + "CmdFlags"
        }, true, true, false, true);
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
        final String permission = "mf.flags";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
            return;
        }

        final Faction playersFaction = getPlayerFaction(player);

        final boolean show = safeEquals(false, args[0], "get", "show", getText("CmdFlagsShow"));
        final boolean set = safeEquals(false, args[0], "set", getText("CmdFlagsSet"));
        if (show) {
            playersFaction.getFlags().sendFlagList(player);
        } else if (set) {
            if (args.length < 3) {
                player.sendMessage(translate("&c" + getText("UsageFlagsSet")));
            } else {

                final StringBuilder builder = new StringBuilder(); // Send the flag_argument as one String
                for (int i = 2; i < args.length; i++) builder.append(args[i]).append(" ");
                playersFaction.getFlags().setFlag(args[1], builder.toString().trim(), player);

            }
        } else {
            player.sendMessage(translate("&c" + getText("FlagsValidSubCommandsShowSet")));
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