package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VassalizeCommand {

    public void sendVassalizationOffer(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.vassalize")) {

                if (args.length > 1) {

                    String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                    if (targetFaction != null) {

                        if (playersFaction != null) {

                            if (playersFaction.isOwner(player.getUniqueId())) {

                                // make sure player isn't trying to vassalize their own faction
                                if (playersFaction.getName().equalsIgnoreCase(targetFaction.getName())) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeSelf"));
                                    return;
                                }

                                // make sure player isn't trying to vassalize their liege
                                if (targetFaction.getName().equalsIgnoreCase(playersFaction.getLiege())) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeLiege"));
                                    return;
                                }

                                // make sure player isn't trying to vassalize a vassal
                                if (targetFaction.hasLiege()) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeVassal"));
                                    return;
                                }

                                // add faction to attemptedVassalizations
                                playersFaction.addAttemptedVassalization(targetFactionName);

                                // inform all players in that faction that they are trying to be vassalized
                                Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertAttemptedVassalization"), playersFaction.getName(), playersFaction.getName()));

                                // inform all players in players faction that a vassalization offer was sent
                                Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertFactionAttemptedToVassalize"), targetFactionName));

                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                        }
                    }
                    else {
                        // faction doesn't exist, send message
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageVassalize"));
                }

            }
            else {
                // send perm message
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.vassalize"));
            }
        }

    }

}
