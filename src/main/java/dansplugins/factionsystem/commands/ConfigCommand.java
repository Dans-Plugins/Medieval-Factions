package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand extends SubCommand {

    public ConfigCommand() {
        super(new String[]{
                "config", LOCALE_PREFIX + "CmdConfig"
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
        if (!(checkPermissions(sender, "mf.config", "mf.admin"))) {
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
            return;
        }
        final boolean show = safeEquals(false, args[0], "get", "show", getText("CmdConfigShow"));
        final boolean set = safeEquals(false, args[0], "set", getText("CmdConfigSet"));
        if (show) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /mf config show ( 1 | 2 )"); // TODO: abstract out into locale message
                return;
            }

            int page = getIntSafe(args[1], -1);

            if (page == -1) {
                // TODO: use integer required locale message here
                return;
            }

            switch(page) {
                case 1:
                    configManager.sendPageOneOfConfigList(sender);
                    break;
                case 2:
                    configManager.sendPageTwoOfConfigList(sender);
                    break;
                default:
                    sender.sendMessage("Usage: /mf config show ( 1 | 2 )"); // TODO: abstract out into locale message
                    return;
            }
        }
        else if (set) {
            if (args.length < 3) {
                sender.sendMessage(translate("&c" + getText("UsageConfigSet")));
            }
            else {
                configManager.setConfigOption(args[1], args[2], sender);
            }
        }
        else {
            sender.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
        }
    }

}
