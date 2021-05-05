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

public class AllyCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AllyCommand() {
        super(new String[] {
                "ally", "Locale_CmdAlly"
        });
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
        if (!isPlayer(sender)) { // Is the CommandSender a player?
            sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
            return;
        }
        final Player player = (Player) sender; // Cast to a Player.
        final String permission = "mf.ally";
        if (!player.hasPermission(permission)) { // Does the player have permission to add a law?
            player.sendMessage(translate("&c" + getText("PermissionNeeded", permission)));
            return;
        }
        if (args.length == 0) { // Check if they have provided any strings beyond "Add Law".
            player.sendMessage(translate("&c" + getText("UsageAddLaw")));
            return;
        }
        // Obtain the Player's Faction.
        final Faction playersFaction = getPlayerFaction(player);
        if (playersFaction == null) { // If the Faction is null, they're not in a Faction.
            player.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
            return;
        }
        if (!(playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId()))) {
            // They need to be the Owner or Officer to do this.
            player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
            return;
        }
        // Retrieve the Faction from the given arguments.
        final Faction faction = getFaction(String.join(" ", args));
        if (faction == null) { // The faction needs to exist to ally
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (faction == playersFaction) { // The faction can't be itself
            player.sendMessage(translate("&c" + getText("CannotAllyWithSelf")));
            return;
        }
        if (playersFaction.isAlly(faction.getName())) { // No need to allow them to ally if they're already allies.
            player.sendMessage(translate("&c" + getText("FactionAlreadyAlly")));
            return;
        }
        if (playersFaction.isRequestedAlly(faction.getName()) || playersFaction.isEnemy(faction.getName())) {
            // Already requested to ally, why you try spam? :O
            player.sendMessage(translate("&c" + getText("AlertAlreadyRequestedAlliance")));
            return;
        }
        playersFaction.requestAlly(faction.getName()); // Send the request.
        player.sendMessage(translate("&a" + getText("AttemptedAlliance", faction.getName())));
        messageFaction(
                playersFaction,
                translate("&a" + getText("AlertAttemptedAlliance", playersFaction.getName(), faction.getName()))
        );
        // Is the playersFaction and the target Faction requesting to Ally each other?
        if (playersFaction.isRequestedAlly(faction.getName()) && faction.isRequestedAlly(playersFaction.getName())) {
            // Then ally them!
            playersFaction.addAlly(faction.getName());
            faction.addAlly(playersFaction.getName());
            // Message player's Faction!
            messageFaction(playersFaction, translate("&a" + getText("AlertNowAlliedWith", faction.getName())));
            // Message target Faction!
            messageFaction(faction, translate("&a" + getText("AlertNowAlliedWith", playersFaction.getName())));
        }
    }

    @Deprecated
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
