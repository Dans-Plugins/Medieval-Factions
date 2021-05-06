package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ConfigManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.ChatColor;
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
        if (!(checkPermissions(sender, "mf.config", "mf.admin"))) return;
        if (args.length == 0) {
            sender.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
            return;
        }
        final boolean show = safeEquals(false, args[0], "get", "show", getText("CmdConfigShow"));
        final boolean set = safeEquals(false, args[0], "set", getText("CmdConfigSet"));
        if (show) config.sendConfigList(sender);
        else if (set) {
            if (args.length == 1) sender.sendMessage(translate("&c" + getText("UsageConfigSet")));
            else config.setConfigOption(args[1], args[2], sender);
        } else sender.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
    }

    @Deprecated
    public boolean handleConfigAccess(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!(player.hasPermission("mf.config") || player.hasPermission("mf.admin"))) {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.config"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("ValidSubCommandsShowSet"));
            return false;
        }

        if (args[1].equalsIgnoreCase("show") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdConfigShow"))) {
            // no further arguments needed, list config
            ConfigManager.getInstance().sendConfigList(player);
            return true;
        }

        if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdConfigSet"))) {

            // two more arguments needed
            if (args.length > 3) {

                String option = args[2];
                String value = args[3];

                ConfigManager.getInstance().setConfigOption(option, value, player);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageConfigSet"));
                return false;
            }

        }

        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("ValidSubCommandsShowSet"));

        return false;
    }

}
