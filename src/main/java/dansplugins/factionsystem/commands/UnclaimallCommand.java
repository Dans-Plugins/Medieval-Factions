package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimallCommand {

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
                        DynmapManager.updateClaims();
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
                        DynmapManager.updateClaims();
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
