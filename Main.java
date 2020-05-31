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
		if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("medievalfactions")) {
			if (!(sender instanceof Player)) { // if sender is not a player
				sender.sendMessage("Alert: Can't be used by console.");
				return true;
			}
			else { // if sender is a player
				
				Player player = (Player) sender;
				
				if (args[0].equalsIgnoreCase("help")) {
					sendHelpMessage(player);
					return true;
				}
				else {
					player.sendMessage("Incorrect usage. Try /mf help!");
				}

			}
		}
		return false;
	}
	
	public void sendHelpMessage(Player player) {
		player.sendMessage("Nothing to help with yet.");
	}
	
}
