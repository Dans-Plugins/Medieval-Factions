package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand {

    public boolean declareIndependence(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.declareindependence")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.declareindependence"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!playersFaction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        if (!playersFaction.hasLiege()) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotAVassalOfAFaction"));
            return false;
        }

        Faction targetFaction = PersistentData.getInstance().getFaction(playersFaction.getLiege());

        // break vassal agreement
        targetFaction.removeVassal(playersFaction.getName());
        playersFaction.setLiege("none");

        // add enemy to declarer's faction's enemyList and the enemyLists of its allies
        playersFaction.addEnemy(targetFaction.getName());

        // add declarer's faction to new enemy's enemyList
        targetFaction.addEnemy(playersFaction.getName());

        Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("HasDeclaredIndependence"), playersFaction.getName(), targetFaction.getName()));
        return true;
    }

}
