/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionLeaveEvent;
import dansplugins.factionsystem.utils.Logger;

/**
 * @author Callum Johnson
 */
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
            Logger.getInstance().debug("Leave event was cancelled.");
            return;
        }

        if (faction.isOfficer(player.getUniqueId())) faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        ephemeral.getPlayersInFactionChat().remove(player.getUniqueId()); // Remove from Faction Chat.
        faction.removeMember(player.getUniqueId());
        player.sendMessage(translate("&b" + getText("AlertLeftFaction")));
        messageFaction(faction, translate("&a" + player.getName() + " has left " + faction.getName()));

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