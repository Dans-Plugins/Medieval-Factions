package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DescCommand {

    public boolean setDescription(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.desc")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isOwner(player.getUniqueId())) {
                        owner = true;
                        if (args.length > 1) {

                            // set arg[1] - args[args.length-1] to be the description with spaces put in between
                            String newDesc = "";
                            for (int i = 1; i < args.length; i++) {
                                newDesc = newDesc + args[i];
                                if (!(i == args.length - 1)) {
                                    newDesc = newDesc + " ";
                                }
                            }

                            faction.setDescription(newDesc);
                            player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("DescriptionSet"));
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageDesc"));
                            return false;
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionDesc"));
                return false;
            }
        }
        return false;
    }

}
