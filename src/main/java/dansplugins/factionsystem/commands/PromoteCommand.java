/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

/**
 * @author Callum Johnson
 */
public class PromoteCommand extends SubCommand {

    public PromoteCommand() {
        super(new String[]{
                "promote", LOCALE_PREFIX + "CmdPromote"
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
        final String permission = "mf.promote";
        if (!(checkPermissions(player, permission))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsagePromote")));
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                return;
            }
        }
        if (!faction.isMember(targetUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotMemberOfFaction")));
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerAlreadyOfficer")));
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            player.sendMessage(translate("&c" + getText("CannotPromoteSelf")));
            return;
        }
        if (faction.addOfficer(targetUUID)) {
            player.sendMessage(translate("&a" + getText("PlayerPromoted")));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(translate("&a" + getText("PromotedToOfficer")));
            }
        } else {
            player.sendMessage(translate("&c" +
                    getText("PlayerCantBePromotedBecauseOfLimit", faction.calculateMaxOfficers())));
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