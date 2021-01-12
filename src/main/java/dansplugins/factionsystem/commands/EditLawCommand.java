package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditLawCommand {

    public void editLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.editlaw")) ) {

            Player player = (Player) sender;

            if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        int lawToEdit = Integer.parseInt(args[1]) - 1;
                        String newLaw = "";
                        for (int i = 2; i < args.length; i++) {
                            newLaw = newLaw + args[i] + " ";
                        }

                        if (playersFaction.editLaw(lawToEdit, newLaw)) {
                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawEdited"));
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageEditLaw"));
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionEditLaw"));
        }
    }

}
