package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BreakAllianceCommand {

    public boolean breakAlliance(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (player.hasPermission("mf.breakalliance")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.breakalliance"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageBreakAlliance"));
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

        String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (playersFaction.getName().equalsIgnoreCase(targetFactionName)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotBreakAllianceWithSelf"));
            return false;
        }

        Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

        if (targetFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
            return false;
        }

        if (!playersFaction.isAlly(targetFactionName)) {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertNotAllied"), targetFaction.getName()));
            return false;
        }

        playersFaction.removeAlly(targetFactionName);
        Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("AllianceBrokenWith"), targetFaction.getName()));

        targetFaction.removeAlly(playersFaction.getName());
        Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("AlertAllianceHasBeenBroken"), playersFaction.getName()));

        return true;
    }
}
