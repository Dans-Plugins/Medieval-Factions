package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckAccessCommand {

    public boolean checkAccess(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.checkaccess")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.checkaccess"));
            return false;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                EphemeralData.getInstance().getPlayersCheckingAccess().remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("Cancelled"));
                return true;
            }
            else {
                return false;
            }
        }

        if (!EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
            EphemeralData.getInstance().getPlayersCheckingAccess().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("RightClickCheckAccess"));
        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlreadyEnteredCheckAccess"));
        }

        return true;
    }

}
