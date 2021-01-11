package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand {

    public void declareIndependence(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.declareindependence")) {

                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                    if (playersFaction != null) {
                        // if faction has liege
                        if (playersFaction.hasLiege()) {

                            Faction targetFaction = PersistentData.getInstance().getFaction(playersFaction.getLiege());

                            // if owner of faction
                            if (playersFaction.isOwner(player.getUniqueId())) {

                                // break vassal agreement
                                targetFaction.removeVassal(playersFaction.getName());
                                playersFaction.setLiege("none");

                                // add enemy to declarer's faction's enemyList and the enemyLists of its allies
                                playersFaction.addEnemy(targetFaction.getName());

                                // add declarer's faction to new enemy's enemyList
                                targetFaction.addEnemy(playersFaction.getName());

                                Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("HasDeclaredIndependence"), playersFaction.getName(), targetFaction.getName()));
                           }
                            else {
                                // tell player they must be owner
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotAVassalOfAFaction"));
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                    }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionDeclareIndependence"));
            }
        }

    }

}
