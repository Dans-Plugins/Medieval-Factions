package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllyCommand {

    public void requestAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.ally")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        // player is able to do this command

                        if (args.length > 1) {
                            String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                            Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                            if (!playersFaction.getName().equalsIgnoreCase(targetFactionName)) {

                                if (targetFaction != null) {

                                    if (!playersFaction.isAlly(targetFactionName)) {
                                        // if not already ally

                                        if (!playersFaction.isRequestedAlly(targetFactionName)) {
                                            // if not already requested

                                            if (!playersFaction.isEnemy(targetFactionName)) {

                                                playersFaction.requestAlly(targetFactionName);
                                                player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AttemptedAlliance"), targetFactionName));

                                                Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertAttemptedAlliance"), playersFaction.getName(), targetFactionName));

                                                if (playersFaction.isRequestedAlly(targetFactionName) && targetFaction.isRequestedAlly(playersFaction.getName())) {
                                                    // ally factions
                                                    playersFaction.addAlly(targetFactionName);
                                                    PersistentData.getInstance().getFaction(targetFactionName).addAlly(playersFaction.getName());
                                                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertNowAlliedWith") + targetFactionName + "!");
                                                    Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNowAlliedWith"), playersFaction.getName()));
                                                }
                                            }
                                            else {
                                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionCurrentlyEnemyMustMakePeace"));
                                            }

                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyRequestedAlliance"));
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAlreadyAlly"));
                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotAllyWithSelf"));
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageAlly"));
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionAlly"));
            }
        }
    }
}
