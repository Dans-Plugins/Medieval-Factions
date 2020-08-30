package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

import java.util.UUID;

public class PowerCommand {

    Main main = null;

    public PowerCommand(Main plugin) {
        main = plugin;
    }

    public boolean powerCheck(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.power") || sender.hasPermission("mf.default"))
            {
            	if (args.length > 1)
            	{
            		UUID playerUUID = findUUIDBasedOnPlayerName(args[1]);
            		if (playerUUID != null)
            		{
	            		PlayerPowerRecord record = getPlayersPowerRecord(playerUUID, main.playerPowerRecords);
	                    player.sendMessage(ChatColor.AQUA + args[1] + "'s current power level is " + record.getPowerLevel() + "/" + record.maxPower() + ".");
	                    return true;
            		}
            		else
            		{
            			sender.sendMessage(ChatColor.RED + "Sorry! Player by the name of '" + args[1] + "' could not be found.");
            			return false;
            		}
            	}
            	else
            	{
                    PlayerPowerRecord record = getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords);
                    player.sendMessage(ChatColor.AQUA + "Your current power level is " + record.getPowerLevel() + "/" + record.maxPower() + ".");
                    return true;
            	}
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.power'");
            }
        }
        return false;
    }
}
