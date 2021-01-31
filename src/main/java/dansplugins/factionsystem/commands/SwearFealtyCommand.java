package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwearFealtyCommand {

    public void swearFealty(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.swearfealty")) {

                if (args.length > 1) {

                    String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                    if (targetFaction != null) {

                        if (playersFaction != null) {
                            // if offered vassalization
                            if (targetFaction.hasBeenOfferedVassalization(playersFaction.getName())) {

                                // if owner of faction
                                if (playersFaction.isOwner(player.getUniqueId())) {

                                    // set vassal
                                    targetFaction.addVassal(playersFaction.getName());
                                    targetFaction.removeAttemptedVassalization(playersFaction.getName());

                                    // inform target faction that they have a new vassal
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertFactionHasNewVassal"), playersFaction.getName()));

                                    // set liege
                                    playersFaction.setLiege(targetFaction.getName());

                                    // inform players faction that they have a new liege
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertFactionHasBeenVassalized"), targetFactionName));
                                }
                                else {
                                    // tell player they must be owner
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                                }

                            }
                            else {
                                // tell player they haven't offered vassalization to their faction
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotOfferedVassalizationBy"));
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
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageSwearFealty"));
                }

            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.swearfealty"));
            }
        }

    }

}
