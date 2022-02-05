/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class InfoCommand extends SubCommand {

    public InfoCommand() {
        super(new String[]{
                "info", LOCALE_PREFIX + "CmdInfo"
        }, false);
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
        final String permission = "mf.info";
        if (!(checkPermissions(sender, permission))) return;
        final Faction target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            target = getPlayerFaction(sender);
            if (target == null) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                return;
            }
        } else {
            target = getFaction(String.join(" ", args));
            if (target == null) {
                sender.sendMessage(translate("&c" + getText("FactionNotFound")));
                return;
            }
        }
        Messenger.getInstance().sendFactionInfo(sender, target, target.getClaimedChunks().size());
    }
}