package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.IFaction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllyCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AllyCommand() {
        super(new String[] {
                "ally", LOCALE_PREFIX + "CmdAlly"
        }, true, true, true, false);
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
        final String permission = "mf.ally";
        if (!checkPermissions(player, permission)) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageAlly")));
            return;
        }
        // Retrieve the Faction from the given arguments.
        final IFaction otherFaction = getFaction(String.join(" ", args));
        if (otherFaction == null) { // The faction needs to exist to ally
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (otherFaction == faction) { // The faction can't be itself
            player.sendMessage(translate("&c" + getText("CannotAllyWithSelf")));
            return;
        }
        if (faction.isAlly(otherFaction.getName())) { // No need to allow them to ally if they're already allies.
            player.sendMessage(translate("&c" + getText("FactionAlreadyAlly")));
            return;
        }
        if (faction.isRequestedAlly(otherFaction.getName()) || faction.isEnemy(otherFaction.getName())) {
            // Already requested to ally, why you try spam? :O
            player.sendMessage(translate("&c" + getText("AlertAlreadyRequestedAlliance")));
            return;
        }
        faction.requestAlly(otherFaction.getName()); // Send the request.
        messageFaction(
                faction,
                translate("&a" + getText("AlertAttemptedAlliance", faction.getName(), otherFaction.getName()))
        );
        messageFaction(
                otherFaction,
                translate("&a" + getText("AlertAttemptedAlliance", faction.getName(), otherFaction.getName()))
        );
        // Is the playersFaction and the target Faction requesting to Ally each other?
        if (faction.isRequestedAlly(otherFaction.getName()) && otherFaction.isRequestedAlly(faction.getName())) {
            // Then ally them!
            faction.addAlly(otherFaction.getName());
            otherFaction.addAlly(faction.getName());
            // Message player's Faction!
            messageFaction(faction, translate("&a" + getText("AlertNowAlliedWith", otherFaction.getName())));
            // Message target Faction!
            messageFaction(otherFaction, translate("&a" + getText("AlertNowAlliedWith", faction.getName())));
            // remove alliance requests
            faction.removeAllianceRequest(otherFaction.getName());
            otherFaction.removeAllianceRequest(faction.getName());
        }
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

}
