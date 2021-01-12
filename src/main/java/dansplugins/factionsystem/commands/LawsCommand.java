package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LawsCommand {

    public void showLawsToPlayer(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.laws")) ) {

            Player player = (Player) sender;

            Faction faction = null;

            if (args.length == 1) {
                faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
            }
            else {
                String target = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                boolean exists = false;
                for (Faction f : PersistentData.getInstance().getFactions()) {
                    if (f.getName().equalsIgnoreCase(target)) {
                        faction = PersistentData.getInstance().getFaction(target);
                        exists = true;
                    }
                }
                if (!exists) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    return;
                }
            }

            if (faction != null) {

                if (faction.getNumLaws() != 0) {

                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("LawsTitle"), faction.getName()));

                    // list laws
                    int counter = 1;
                    for (String law : faction.getLaws()) {
                        player.sendMessage(ChatColor.AQUA + "" + counter + ". " + faction.getLaws().get(counter - 1));
                        counter++;
                    }

                }
                else {
                    if (args.length == 1) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNoLaws"));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionDoesNotHaveLaws"));
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));

            }

        }
        else {
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionLaws"));
        }
    }

}
