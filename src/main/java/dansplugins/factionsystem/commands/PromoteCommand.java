package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class PromoteCommand {

    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.promote")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.isMember(playerUUID)) {
                                        if (faction.isOfficer(playerUUID)) {
                                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerAlreadyOfficer"));
                                            return;
                                        }

                                        if(faction.addOfficer(playerUUID)){
                                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerPromoted"));

                                            try {
                                                Player target = getServer().getPlayer(args[1]);
                                                target.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PromotedToOfficer"));
                                            }
                                            catch(Exception ignored) {

                                            }
                                        } else {
                                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PlayerCantBePromotedBecauseOfLimit"), faction.calculateMaxOfficers()));
                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotMemberOfFaction"));
                                    }

                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsagePromote"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.promote"));
            }
        }
    }
}
