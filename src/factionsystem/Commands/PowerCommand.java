package factionsystem.Commands;

import factionsystem.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PowerCommand {

    public static void powerCheck(CommandSender sender, ArrayList<PlayerPowerRecord> powerRecords) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            for (PlayerPowerRecord record : powerRecords) {
                if (record.getPlayerName().equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.AQUA + "Your current power level is " + record.getPowerLevel());
                }
            }
        }
    }
}
