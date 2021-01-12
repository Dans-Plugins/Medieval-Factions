package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand {

    public boolean joinFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.join")) {
                if (args.length > 1) {

                    // creating name from arguments 1 to the last one
                    String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(factionName)) {
                            if (faction.isInvited(player.getUniqueId())) {

                                // join if player isn't in a faction already
                                if (!(PersistentData.getInstance().isInFaction(player.getUniqueId()))) {
                                    faction.addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                    faction.uninvite(player.getUniqueId());
                                    try {
                                        Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("HasJoined"), player.getName(), faction.getName()));
                                    } catch (Exception ignored) {

                                    }
                                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertJoinedFaction"));
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyInFaction"));
                                    return false;
                                }

                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotInvitedToFaction"));
                                return false;
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageJoin"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionJoin"));
                return false;
            }
        }
        return false;
    }

}
