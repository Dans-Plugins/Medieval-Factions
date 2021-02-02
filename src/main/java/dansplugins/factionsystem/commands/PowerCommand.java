package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.UUIDChecker;
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
	                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CurrentPowerLevel"), args[1], record.getPowerLevel(),record.maxPower()));
	                    return true;
            		}
            		else
            		{
            			sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PlayerByNameNotFound"), args[1]));
            			return false;
            		}
            	}
            	else
            	{
                    PlayerPowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertCurrentPowerLevel"), record.getPowerLevel(), record.maxPower()));
                    return true;
            	}
            }
            else {
				sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.power"));
            }
        }
        return false;
    }
}
