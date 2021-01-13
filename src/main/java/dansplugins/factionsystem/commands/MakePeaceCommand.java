package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MakePeaceCommand {

    public void makePeace(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.makepeace")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        // player is able to do this command

                        if (args.length > 1) {
                            String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                            Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                            if (!playersFaction.getName().equalsIgnoreCase(targetFactionName)) {

                                if (targetFaction != null) {

                                    if (!playersFaction.isTruceRequested(targetFactionName)) {
                                        // if not already requested

                                        if (playersFaction.isEnemy(targetFactionName)) {

                                            playersFaction.requestTruce(targetFactionName);
                                            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AttemptedPeace"), targetFactionName));

                                            Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("HasAttemptedToMakePeaceWith"), playersFaction.getName(), targetFactionName));

                                            if (playersFaction.isTruceRequested(targetFactionName) && targetFaction.isTruceRequested(playersFaction.getName())) {
                                                // remove requests in case war breaks out again and they need to make peace aagain
                                                playersFaction.removeRequestedTruce(targetFactionName);
                                                targetFaction.removeRequestedTruce(playersFaction.getName());

                                                // make peace between factions
                                                playersFaction.removeEnemy(targetFactionName);
                                                PersistentData.getInstance().getFaction(targetFactionName).removeEnemy(playersFaction.getName());
                                                Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNowAtPeaceWith"), playersFaction.getName(), targetFactionName));
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotEnemy"));
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyRequestedPeace"));
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotMakePeaceWithSelf"));
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageMakePeace"));
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
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.makepeace"));
            }
        }
    }
}
