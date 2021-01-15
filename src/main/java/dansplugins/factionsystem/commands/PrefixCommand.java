package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrefixCommand {

    public boolean changePrefix(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.prefix")) {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.prefix"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsagePrefix"));
            return false;
        }

        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (faction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!faction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        String newPrefix = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (isPrefixTaken(newPrefix)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PrefixTaken"));
            return false;
        }

        faction.setPrefix(newPrefix);

        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PrefixSet"));

        return true;
    }

    private boolean isPrefixTaken(String prefix) {
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.getPrefix().equalsIgnoreCase(prefix)) {
                return true;
            }
        }
        return false;
    }

}
