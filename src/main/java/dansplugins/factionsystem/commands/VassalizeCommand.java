package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VassalizeCommand {

    public boolean sendVassalizationOffer(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.vassalize")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.vassalize"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageVassalize"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        String targetFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
        Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

        if (targetFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
            return false;
        }

        if (!playersFaction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        // make sure player isn't trying to vassalize their own faction
        if (playersFaction.getName().equalsIgnoreCase(targetFaction.getName())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeSelf"));
            return false;
        }

        // make sure player isn't trying to vassalize their liege
        if (targetFaction.getName().equalsIgnoreCase(playersFaction.getLiege())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeLiege"));
            return false;
        }

        // make sure player isn't trying to vassalize a vassal
        if (targetFaction.hasLiege()) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotVassalizeVassal"));
            return false;
        }

        // make sure this vassalization won't result in a vassalization loop
        if (willVassalizationResultInLoop(playersFaction, targetFaction)) {
            System.out.println("DEBUG: vassalization was cancelled due to potential loop"); // TODO: replace with message
            return false;
        }

        // add faction to attemptedVassalizations
        playersFaction.addAttemptedVassalization(targetFactionName);

        // inform all players in that faction that they are trying to be vassalized
        Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + String.format(LocaleManager.getInstance().getText("AlertAttemptedVassalization"), playersFaction.getName(), playersFaction.getName()));

        // inform all players in players faction that a vassalization offer was sent
        Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertFactionAttemptedToVassalize"), targetFactionName));

        return true;
    }

    private boolean willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {

        Faction current = vassalizer;

        while (true) {
            String liegeName = current.getLiege();

            if (liegeName.equalsIgnoreCase("none")) {
                // no loop will be formed
                return false;
            }

            if (liegeName.equalsIgnoreCase(potentialVassal.getName())) {
                // loop will be formed
                return true;
            }

            Faction liege = PersistentData.getInstance().getFaction(liegeName); // liege should never be null
            current = liege;
        }
    }

}
