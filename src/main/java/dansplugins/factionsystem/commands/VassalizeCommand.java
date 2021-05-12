package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VassalizeCommand extends SubCommand {

    public VassalizeCommand() {
        super(new String[] {
                "Vassalize", LOCALE_PREFIX + "CmdVassalize"
        }, true, true, false, true);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.vassalize";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageVassalize")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        // make sure player isn't trying to vassalize their own faction
        if (faction.getName().equalsIgnoreCase(target.getName())) {
            player.sendMessage(translate("&c" + getText("CannotVassalizeSelf")));
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getName().equalsIgnoreCase(faction.getLiege())) {
            player.sendMessage(translate("&c" + getText("CannotVassalizeLiege")));
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            player.sendMessage(translate("&c" + getText("CannotVassalizeVassal")));
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = willVassalizationResultInLoop(faction, target);
        if (loopCheck == 1 || loopCheck == 2) {
            System.out.println("DEBUG: vassalization was cancelled due to potential loop"); // TODO: replace with message
            return;
        }
        // add faction to attemptedVassalizations
        faction.addAttemptedVassalization(target.getName());

        // inform all players in that faction that they are trying to be vassalized
        messageFaction(target, translate("&a" +
                getText("AlertAttemptedVassalization", faction.getName(), faction.getName())));

        // inform all players in players faction that a vassalization offer was sent
        messageFaction(faction, translate("&a" + getText("AlertFactionAttemptedToVassalize", target.getName())));
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }

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
        if (willVassalizationResultInLoop(playersFaction, targetFaction) == 1) { // Fix due to new changes.
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

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            String liegeName = current.getLiege();
            if (liegeName.equalsIgnoreCase("none")) return 0; // no loop will be formed
            if (liegeName.equalsIgnoreCase(potentialVassal.getName())) return 1; // loop will be formed
            current = data.getFaction(liegeName);
            steps++;
        }
        return 2; // We don't know :/
    }

}
