package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KickCommand {

    public boolean kickPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.kick")) {
                if (args.length > 1) {
                    boolean owner = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                            owner = true;
                            UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (!(args[1].equalsIgnoreCase(player.getName()))) {
                                    if (!(playerUUID.equals(faction.getOwner()))) {

                                        if (faction.isOfficer(playerUUID)) {
                                            faction.removeOfficer(playerUUID);
                                        }

                                        EphemeralData.getInstance().getPlayersInFactionChat().remove(playerUUID);

                                        faction.removeMember(playerUUID, PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                        try {
                                            Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + String.format(LocaleManager.getInstance().getText("HasBeenKickedFrom"),  args[1], faction.getName()));
                                        } catch (Exception ignored) {

                                        }
                                        try {
                                            Player target = Bukkit.getServer().getPlayer(args[1]);
                                            target.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertKicked"), player.getName()));
                                        } catch (Exception ignored) {

                                        }
                                        return true;
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotKickOwner"));
                                        return false;
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotKickSelf"));
                                    return false;
                                }

                            }
                        }
                    }

                    if (!owner) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                        return false;
                    }

                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageKick"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionKick"));
                return false;
            }
        }
        return false;
    }

}
