package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand {

    public boolean toggleFactionChat(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.chat")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.chat"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!EphemeralData.getInstance().getPlayersInFactionChat().contains(player.getUniqueId())) {
            EphemeralData.getInstance().getPlayersInFactionChat().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NowSpeakingInFactionChat"));
        }
        else {
            EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NoLongerInFactionChat"));
        }

        return true;

    }

}
