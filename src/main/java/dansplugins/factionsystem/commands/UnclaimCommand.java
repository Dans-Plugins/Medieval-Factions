package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimCommand {

    public boolean unclaim(CommandSender sender) {
        if (sender.hasPermission("mf.unclaim")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    ChunkManager.getInstance().removeChunkAtPlayerLocation(player);
                    DynmapManager.getInstance().updateClaims();
                    return true;
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                    return false;
                }

            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.unclaim"));
            return false;
        }
        return false;
    }

}
