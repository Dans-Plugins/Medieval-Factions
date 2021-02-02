package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RevokeAccessCommand {

    public void revokeAccess(CommandSender sender, String[] args) {

        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.revokeaccess")) ) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                        EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Cancelled"));
                        return;
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageRevokeAccess"));
                return;
            }

            if (!EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                EphemeralData.getInstance().getPlayersRevokingAccess().put(player.getUniqueId(), UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]));
                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("RightClickRevokeAccess"));
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlreadyEnteredRevokeAccess"));
            }

        }

    }

}
