package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MakePeaceCommand extends SubCommand {

    public MakePeaceCommand() {
        super(new String[] {
            "makepeace", "mp", LOCALE_PREFIX + "CmdMakePeace"
        }, true, true, true, false);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.makepeace";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageMakePeace")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (target == faction) {
            player.sendMessage(translate("&c" + getText("CannotMakePeaceWithSelf")));
            return;
        }
        if (faction.isTruceRequested(target.getName())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyRequestedPeace")));
            return;
        }
        if (!faction.isEnemy(target.getName())) {
            player.sendMessage(translate("&c" + getText("FactionNotEnemy")));
            return;
        }
        faction.requestTruce(target.getName());
        player.sendMessage(translate("&c" + getText("AttemptedPeace", target.getName())));
        messageFaction(target,
                translate("&a" + getText("HasAttemptedToMakePeaceWith", faction.getName(), target.getName())));
        if (faction.isTruceRequested(target.getName()) && target.isTruceRequested(faction.getName())) {
            // remove requests in case war breaks out again and they need to make peace aagain
            faction.removeRequestedTruce(target.getName());
            target.removeRequestedTruce(faction.getName());

            // make peace between factions
            faction.removeEnemy(target.getName());
            target.removeEnemy(faction.getName());

            // Notify
            messageServer(translate("&a" + getText("AlertNowAtPeaceWith", faction.getName(), target.getName())));
        }
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }

    @Deprecated
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
