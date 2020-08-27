package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersPowerRecord;

public class PowerCommand {

    Main main = null;

    public PowerCommand(Main plugin) {
        main = plugin;
    }

    public void powerCheck(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerPowerRecord record = getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords);
            player.sendMessage(ChatColor.AQUA + "Your current power level is " + record.getPowerLevel() + "/" + record.getMaxPower() + ".");
        }
    }
}
