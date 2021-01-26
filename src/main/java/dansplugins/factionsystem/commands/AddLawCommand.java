package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddLawCommand {

    public boolean addLaw(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (player.hasPermission("mf.addlaw")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.addlaw"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageAddLaw"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!playersFaction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        String newLaw = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
        playersFaction.addLaw(newLaw);
        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawAdded"));
        return true;
    }
}
