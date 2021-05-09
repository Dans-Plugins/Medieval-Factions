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

public class GrantIndependenceCommand extends SubCommand {

    public GrantIndependenceCommand() {
        super(new String[]{
                "GrantIndependence", "GI", LOCALE_PREFIX + "CmdGrantIndependence"
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
        if (!(checkPermissions(player, "mf.grantindependence"))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantIndependence")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!target.isLiege(this.faction.getName())) {
            player.sendMessage(translate("&c" + getText("FactionIsNotVassal")));
            return;
        }
        target.setLiege("none");
        this.faction.removeVassal(target.getName());
        // inform all players in that faction that they are now independent
        messageFaction(target, translate("&a" + getText("AlertGrantedIndependence", faction.getName())));
        // inform all players in players faction that a vassal was granted independence
        messageFaction(faction, translate("&a" + getText("AlertNoLongerVassalFaction", target.getName())));
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
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertGrantedIndependence"), playersFaction.getName()));

                                    // inform all players in players faction that a vassal was granted independence
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNoLongerVassalFaction"), targetFaction.getName()));
                                } else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionIsNotVassal"));
                                }

                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    }

                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageGrantIndependence"));
                }

            } else {
                // send perm message
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.grantindependence"));
            }
        }

    }

}
