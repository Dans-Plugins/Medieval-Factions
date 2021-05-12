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

public class SwearFealtyCommand extends SubCommand {

    public SwearFealtyCommand() {
        super(new String[] {
                "swearfealty", LOCALE_PREFIX + "CmdSwearFealty", "sf"
        }, true, true, false, true);
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
        final String permission = "mf.swearfealty";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageSwearFealty")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!target.hasBeenOfferedVassalization(faction.getName())) {
            player.sendMessage(translate("&c" + getText("AlertNotOfferedVassalizationBy")));
            return;
        }
        // set vassal
        target.addVassal(faction.getName());
        target.removeAttemptedVassalization(faction.getName());

        // set liege
        faction.setLiege(target.getName());

        // inform target faction that they have a new vassal
        messageFaction(target, translate("&a" + getText("AlertFactionHasNewVassal", faction.getName())));

        // inform players faction that they have a new liege
        messageFaction(faction, translate("&a" + getText("AlertFactionHasBeenVassalized", target.getName())));
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
