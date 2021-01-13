package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddLawCommand {

    public void addLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.addlaw")) ) {

            Player player = (Player) sender;

            if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        String newLaw = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                        playersFaction.addLaw(newLaw);

                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawAdded"));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageAddLaw"));
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.addlaw"));
        }

    }
}
