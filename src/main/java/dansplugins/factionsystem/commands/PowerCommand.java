package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PowerCommand {

    public boolean powerCheck(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.power"))
            {
            	if (args.length > 1)
            	{
            		UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
            		if (playerUUID != null)
            		{
	            		PlayerPowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(playerUUID);
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
                    PlayerPowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
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
