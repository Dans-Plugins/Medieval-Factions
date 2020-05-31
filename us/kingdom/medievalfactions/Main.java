package us.kingdom.medievalfactions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	@Override
	public void onEnable() {
		
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("mftest")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Alert: Can't be used by console.");
				return true;
			}
			else {
				Player player = (Player) sender;
				if (player.hasPermission("FlyCommand.fly")) {
					player.setAllowFlight(!player.getAllowFlight());
					player.sendMessage("Flight toggled to " + player.getAllowFlight());
					return true;
				}
				else {
					player.sendMessage("Alert: Permission 'FlyCommand.fly' required.");
				}
			}
		}
		return false;
	}
	
}
