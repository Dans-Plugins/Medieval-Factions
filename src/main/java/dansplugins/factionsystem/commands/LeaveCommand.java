package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand extends SubCommand {

    public LeaveCommand() {
        super(new String[]{"leave", LOCALE_PREFIX + "CmdLeave"}, true, true, false, false);
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
        final String permission = "mf.leave";
        if (!(checkPermissions(player, permission))) return;
        final boolean isOwner = this.faction.isOwner(player.getUniqueId());
        if (isOwner) {
            new DisbandCommand().execute((CommandSender) player, args, key); // Disband the Faction.
            return;
        }
        FactionLeaveEvent leaveEvent = new FactionLeaveEvent(faction, player);
        Bukkit.getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) {
            // TODO Locale Message
            return;
        }

        if (faction.isOfficer(player.getUniqueId())) faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        ephemeral.getPlayersInFactionChat().remove(player.getUniqueId()); // Remove from Faction Chat.
        faction.removeMember(player.getUniqueId(), data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        player.sendMessage(translate("&b" + getText("AlertLeftFaction")));
        messageFaction(faction, translate("&a" + player.getName() + " has left " + faction.getName()));
        // TODO Edit this message.

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
