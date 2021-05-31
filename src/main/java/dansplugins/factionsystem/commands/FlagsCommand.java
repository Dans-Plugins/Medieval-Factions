package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlagsCommand extends SubCommand {

    public FlagsCommand() {
        super(new String[]{
                "flags", LOCALE_PREFIX + "CmdFlags"
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
        if (!(checkPermissions(player, "mf.flags", "mf.admin"))) { // TODO: add permission to plugin.yml
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("FlagsValidSubCommandsShowSet"))); // TODO: add string key/value pair
            return;
        }

        final Faction playersFaction = getPlayerFaction(player);
        if (playersFaction == null) {
            player.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
            return;
        }

        final boolean show = safeEquals(false, args[0], "get", "show", getText("CmdFlagsShow")); // TODO: add string key/value pair
        final boolean set = safeEquals(false, args[0], "set", getText("CmdFlagsSet")); // TODO: add string key/value pair
        if (show) {
            // TODO: send list of flags
        }
        else if (set) {
            if (args.length == 1) {
                player.sendMessage(translate("&c" + getText("UsageFlagsSet"))); // TODO: add string key/value pair
            }
            else {
                // TODO: set flag
            }
        }
        else {
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
