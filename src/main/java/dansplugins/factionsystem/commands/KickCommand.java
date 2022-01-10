package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionKickEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.spigot.tools.UUIDChecker;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class KickCommand extends SubCommand {

    public KickCommand() {
        super(new String[] {
                "kick", LOCALE_PREFIX + "CmdKick"
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
        final String permission = "mf.kick";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageKick")));
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
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("CannotKickSelf")));
            return;
        }
        if (this.faction.isOwner(targetUUID)) {
            player.sendMessage(translate("&c" + getText("CannotKickOwner")));
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            // TODO: add locale message
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        ephemeral.getPlayersInFactionChat().remove(targetUUID);
        faction.removeMember(targetUUID);
        messageFaction(faction, translate("&c" + getText("HasBeenKickedFrom", target.getName(), faction.getName())));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(translate("&c" + getText("AlertKicked", player.getName())));
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