package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GrantIndependenceCommand {

    public void grantIndependence(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.grantindependence")) {

                if (args.length > 1) {

                    String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                    if (targetFaction != null) {

                        if (playersFaction != null) {

                            if (playersFaction.isOwner(player.getUniqueId())) {
                                // if target faction is a vassal
                                if (targetFaction.isLiege(playersFaction.getName())) {
                                    targetFaction.setLiege("none");
                                    playersFaction.removeVassal(targetFaction.getName());

                                    // inform all players in that faction that they are now independent
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertGrantedIndependence"), targetFaction.getName()));

                                    // inform all players in players faction that a vassal was granted independence
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNoLongerVassalFaction"), playersFaction.getName()));
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionIsNotVassal"));
                                }

                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageGrantIndependence"));
                }

            }
            else {
                // send perm message
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.grantindependence"));
            }
        }

    }

}
