package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PowerCommand {

    Main main = null;

    public PowerCommand(Main plugin) {
        main = plugin;
    }

    public void powerCheck(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            for (PlayerPowerRecord record : main.playerPowerRecords) {
                if (record.getPlayerUUID() == player.getUniqueId()) {
                    player.sendMessage(ChatColor.AQUA + "Your current power level is " + record.getPowerLevel());
                }
            }
        }
    }
}
