package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimallCommand extends SubCommand {

    public UnclaimallCommand() {
        super(new String[] {
                "unclaimall", "ua", LOCALE_PREFIX + "CmdUnclaimall"
        }, false);
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
        final Faction faction;
        if (args.length == 0) {
            // Self
            if (!(sender instanceof Player)) {
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            if (!(checkPermissions(sender, "mf.unclaimall"))) return;
            faction = getPlayerFaction(sender);
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                return;
            }
            if (!faction.isOwner(((Player) sender).getUniqueId())) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeOwnerToUseCommand")));
                return;
            }
        } else {
            if (!(checkPermissions(sender, "mf.unclaimall.others", "mf.admin"))) return;
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("FactionNotFound")));
                return;
            }
        }
        // remove faction home
        faction.setFactionHome(null);
        messageFaction(faction, translate("&c" + getText("AlertFactionHomeRemoved")));

        // remove claimed chunks
        chunks.removeAllClaimedChunks(faction.getName(), data.getClaimedChunks());
        dynmap.updateClaims();
        sender.sendMessage(translate("&a" + getText("AllLandUnclaimedFrom", faction.getName())));

        // remove locks associated with this faction
        data.removeAllLocks(faction.getName());
    }

    @Deprecated
    public boolean unclaimAllLand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (player.hasPermission("mf.unclaimall.others") || player.hasPermission("mf.admin")) {

                    String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    Faction faction = PersistentData.getInstance().getFaction(factionName);

                    if (faction != null) {
                        // remove faction home
                        faction.setFactionHome(null);
                        Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + LocaleManager.getInstance().getText("AlertFactionHomeRemoved"));

                        // remove claimed chunks
                        ChunkManager.getInstance().removeAllClaimedChunks(faction.getName(), PersistentData.getInstance().getClaimedChunks());
                        DynmapManager.getInstance().updateClaims();
                        player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AllLandUnclaimedFrom"), factionName));

                        // remove locks associated with this faction
                        PersistentData.getInstance().removeAllLocks(faction.getName());
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.unclaimall.others"));
                    return false;
                }
            }

            if (sender.hasPermission("mf.unclaimall")) {

                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isOwner(player.getUniqueId())) {
                        // remove faction home
                        faction.setFactionHome(null);
                        Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + LocaleManager.getInstance().getText("AlertFactionHomeRemoved"));

                        // remove claimed chunks
                        ChunkManager.getInstance().removeAllClaimedChunks(faction.getName(), PersistentData.getInstance().getClaimedChunks());
                        DynmapManager.getInstance().updateClaims();
                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AllLandUnclaimed"));

                        // remove locks associated with this faction
                        PersistentData.getInstance().removeAllLocks(faction.getName());
                        return true;
                    }
                }
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotInFaction"));
                return false;
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.unclaimall"));
                return false;
            }
        }
        return false;
    }

}
