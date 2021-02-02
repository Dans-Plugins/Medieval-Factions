package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllyCommand {

    public boolean requestAlliance(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.ally")) {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.ally"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageAlly"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!(playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId()))) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
            return false;
        }

        String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (playersFaction.getName().equalsIgnoreCase(targetFactionName)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotAllyWithSelf"));
            return false;
        }

        Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

        if (targetFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
            return false;
        }

        if (playersFaction.isAlly(targetFactionName)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAlreadyAlly"));
            return false;
        }

        if (playersFaction.isRequestedAlly(targetFactionName)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyRequestedAlliance"));
            return false;
        }

        if (playersFaction.isEnemy(targetFactionName)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyRequestedAlliance"));
            return false;
        }

        playersFaction.requestAlly(targetFactionName);
        player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AttemptedAlliance"), targetFactionName));

        Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertAttemptedAlliance"), playersFaction.getName(), targetFactionName));

        if (playersFaction.isRequestedAlly(targetFactionName) && targetFaction.isRequestedAlly(playersFaction.getName())) {
            // ally factions
            playersFaction.addAlly(targetFactionName);
            PersistentData.getInstance().getFaction(targetFactionName).addAlly(playersFaction.getName());
            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNowAlliedWith"), targetFactionName));
            Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertNowAlliedWith"), playersFaction.getName()));
        }

        return true;
    }
}
