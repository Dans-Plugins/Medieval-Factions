/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class TransferCommand extends SubCommand {

    public TransferCommand() {
        super(new String[]{"transfer", LOCALE_PREFIX + "CmdTransfer"}, true, true, false, true);
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
        final String permission = "mf.transfer";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageTransfer")));
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
            player.sendMessage(translate("&c" + getText("PlayerIsNotInYourFaction")));
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("CannotTransferToSelf")));
            return;
        }

        if (faction.isOfficer(targetUUID)) faction.removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        faction.setOwner(targetUUID);
        player.sendMessage(translate("&b" + getText("OwnerShipTransferredTo", args[0])));
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            target.getPlayer().sendMessage(translate("&a" + getText("OwnershipTransferred", faction.getName())));
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