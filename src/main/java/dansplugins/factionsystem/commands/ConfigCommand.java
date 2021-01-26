package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ConfigManager;
import dansplugins.factionsystem.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand {

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
            ConfigManager.getInstance().sendPlayerConfigList(player);
            return true;
        }

        if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdConfigSet"))) {

            // two more arguments needed
            if (args.length > 3) {

                String option = args[2];
                String value = args[3];

                ConfigManager.setConfigOption(option, value, player);
                return true;
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageConfigSet"));
                return false;
            }

        }

        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("ValidSubCommandsShowSet"));
        return false;
    }

}
