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
		if (label.equalsIgnoreCase("test")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Alert: Can't be used by console.");
				return true;
			}
			else {
				Player player = (Player) sender;
				player.sendMessage("Test Complete");
				return true;
			}
		}
		return false;
	}
	
}
