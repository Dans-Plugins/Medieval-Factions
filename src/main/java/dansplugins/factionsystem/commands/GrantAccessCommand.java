package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GrantAccessCommand {

    public void grantAccess(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length > 1) {

                // if args[1] is cancel, cancel this
                if (args[1].equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("CommandCancelled"));
                    return;
                }

                // if not already granting access
                if (!EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
                    // save target name and player name in hashmap in main
                    EphemeralData.getInstance().getPlayersGrantingAccess().put(player.getUniqueId(), UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]));
                    player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("RightClickGrantAccess"), args[1]));
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyGrantingAccess"));
                }
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageGrantAccess"));
            }

        }
    }

}
