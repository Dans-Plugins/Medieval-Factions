package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PowerCommand extends SubCommand {

	public PowerCommand() {
		super(new String[] {
				"power", LOCALE_PREFIX + "CmdPower"
		}, false);
	}

	/**
	 * Method to execute the command for a player.
	 *
	 * @param player who sent the command.
	 * @param args   of the command.
	 * @param key    of the sub-command (e.g. Ally).
	 */
	@Override
	public void execute(Player player, String[] args, String key) {

	}

	/**
	 * Method to execute the command.
	 *
	 * @param sender who sent the command.
	 * @param args   of the command.
	 * @param key    of the command.
	 */
	@Override
	public void execute(CommandSender sender, String[] args, String key) {
		final String permission = "mf.power";
		if (!(checkPermissions(sender, permission))) return;
		final PlayerPowerRecord record;
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
				return;
			}
			record = data.getPlayersPowerRecord(((Player) sender).getUniqueId());
			sender.sendMessage(translate("&b" +
					getText("AlertCurrentPowerLevel", record.getPowerLevel(), record.maxPower())));
			return;
		}
		final UUID target = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
		if (target == null) {
			sender.sendMessage(translate("&c" + getText("PlayerByNameNotFound")));
			return;
		}
		record = data.getPlayersPowerRecord(target);
		sender.sendMessage(translate("&b" +
				getText("CurrentPowerLevel", args[0], record.getPowerLevel(),record.maxPower())));
	}

	@Deprecated
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
