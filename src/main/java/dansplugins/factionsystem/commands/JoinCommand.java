package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand extends SubCommand {

    public JoinCommand() {
        super(new String[] {
                "join", LOCALE_PREFIX + "CmdJoin"
        }, true);
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
        final String permission = "mf.join";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageJoin")));
            return;
        }
        if (data.isInFaction(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyInFaction")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!target.isInvited(player.getUniqueId())) {
            // TODO Locale Message
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            // TODO Locale Message
            return;
        }
        target.addMember(player.getUniqueId(), data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        target.uninvite(player.getUniqueId());
        messageFaction(target, translate("&a" + getText("HasJoined", player.getName(), target.getName())));
        player.sendMessage(translate("&a" + getText("AlertJoinedFaction")));
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
