package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand {

    public void toggleFactionChat(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.chat")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (!EphemeralData.getInstance().getPlayersInFactionChat().contains(player.getUniqueId())) {
                        EphemeralData.getInstance().getPlayersInFactionChat().add(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NowSpeakingInFactionChat"));
                    }
                    else {
                        EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NoLongerInFactionChat"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionChat"));
            }
        }
    }

}
