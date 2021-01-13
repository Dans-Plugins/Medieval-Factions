package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveLawCommand {

    public void removeLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.removelaw")) ) {

            Player player = (Player) sender;

            if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        int lawToRemove = Integer.parseInt(args[1]) - 1;

                        if (playersFaction.removeLaw(lawToRemove)) {
                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawRemoved"));
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageRemoveLaw"));
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.removelaw"));
        }

    }
}
