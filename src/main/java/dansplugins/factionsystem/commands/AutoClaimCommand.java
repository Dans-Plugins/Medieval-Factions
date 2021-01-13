package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoClaimCommand {

    public boolean toggleAutoClaim(CommandSender sender) {
        if (sender.hasPermission("mf.autoclaim")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    boolean owner = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isOwner(player.getUniqueId())) {
                            owner = true;
                            faction.toggleAutoClaim();
                            player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AutoclaimToggled"));
                            return true;
                        }

                    }
                    if (!owner) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                        return false;
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                    return false;
                }

            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.autoclaim"));
            return false;
        }
        return false;
    }

}
