package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class InviteCommand {

    public boolean invitePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.invite")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                            if (args.length > 1) {
                                UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                                // invite if player isn't in a faction already
                                if (!(PersistentData.getInstance().isInFaction(playerUUID))) {
                                    faction.invite(playerUUID);
                                    try {
                                        Player target = Bukkit.getServer().getPlayer(args[1]);
                                        target.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertBeenInvited"), faction.getName(), faction.getName()));
                                    } catch (Exception ignored) {

                                    }
                                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("InvitationSent"));

                                    int seconds = 60 * 60 * 24;

                                    // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
                                    getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
                                        @Override
                                        public void run() {
                                            faction.uninvite(playerUUID);
                                            try {
                                                Player target = Bukkit.getServer().getPlayer(args[1]);
                                                target.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("InvitationExpired"), faction.getName()));
                                            } catch (Exception ignored) {
                                                // player offline
                                            }
                                        }
                                    }, seconds * 20);

                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerAlreadyInFaction"));
                                    return false;
                                }


                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageInvite"));
                                return false;
                            }
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionInvite"));
                return false;
            }
        }
        return false;
    }

}
