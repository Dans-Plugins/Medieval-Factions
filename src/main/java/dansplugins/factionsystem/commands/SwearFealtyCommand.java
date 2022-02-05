/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class SwearFealtyCommand extends SubCommand {

    public SwearFealtyCommand() {
        super(new String[]{
                "swearfealty", LOCALE_PREFIX + "CmdSwearFealty", "sf"
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
        final String permission = "mf.swearfealty";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageSwearFealty")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!target.hasBeenOfferedVassalization(faction.getName())) {
            player.sendMessage(translate("&c" + getText("AlertNotOfferedVassalizationBy")));
            return;
        }
        // set vassal
        target.addVassal(faction.getName());
        target.removeAttemptedVassalization(faction.getName());

        // set liege
        faction.setLiege(target.getName());

        // inform target faction that they have a new vassal
        messageFaction(target, translate("&a" + getText("AlertFactionHasNewVassal", faction.getName())));

        // inform players faction that they have a new liege
        messageFaction(faction, translate("&a" + getText("AlertFactionHasBeenVassalized", target.getName())));
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