package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Subsystems.ConfigSubsystem;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super();
    }

    // args count is at least 1 at this point (/mf config)
    // possible sub-commands are show and set
    public void handleConfigAccess(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.config") || player.hasPermission("mf.admin")) {
                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("show")) {
                        // no further arguments needed, list config
                        MedievalFactions.getInstance().config.sendPlayerConfigList(player);
                        return;
                    }

                    if (args[1].equalsIgnoreCase("set")) {

                        // two more arguments needed
                        if (args.length > 3) {

                            String option = args[2];
                            String value = args[3];

                            ConfigSubsystem.setConfigOption(option, value, player);
                            return;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf config set (option) (value)");
                            return;
                        }

                    }

                    player.sendMessage(ChatColor.RED + "Valid sub-commands: show, set");

                }
                else {
                    player.sendMessage(ChatColor.RED + "Valid sub-commands: show, set");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.config'");
            }
        }

    }

}
