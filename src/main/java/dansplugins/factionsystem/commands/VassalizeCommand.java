/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
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
            Logger.getInstance().log("Vassalization was cancelled due to potential loop");
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