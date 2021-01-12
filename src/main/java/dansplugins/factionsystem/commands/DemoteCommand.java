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

public class DemoteCommand {

    public void demotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.demote")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID officerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (officerUUID != null && faction.isOfficer(officerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.removeOfficer(officerUUID)) {

                                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerDemoted"));

                                        try {
                                            Player target = getServer().getPlayer(officerUUID);
                                            target.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertDemotion"));
                                        }
                                        catch(Exception ignored) {

                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotOfficerOfFaction"));
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageDemote"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionDemote"));
            }
        }
    }
}
