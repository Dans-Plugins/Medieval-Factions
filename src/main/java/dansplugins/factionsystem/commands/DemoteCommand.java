package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DemoteCommand extends SubCommand {

    public DemoteCommand() {
        super(new String[]{
                "demote", LOCALE_PREFIX + "CmdDemote"
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
        final String permission = "mf.demote";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageDemote")));
            return;
        }

        OfflinePlayer demotee = null;
        for (UUID uuid : this.faction.getMemberList()) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() == null) continue;
            if (offlinePlayer.getName().equalsIgnoreCase(args[0])) demotee = offlinePlayer;
        }

        if (demotee == null) {
            player.sendMessage(translate("&c" + getText("PlayerByNameNotFound", args[0])));
            return;
        }

        if (demotee.getUniqueId() == player.getUniqueId()) {
            player.sendMessage(translate("&c" + getText("CannotDemoteSelf")));
            return;
        }

        if (!this.faction.isOfficer(demotee.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotOfficerOfFaction")));
            return;
        }

        faction.removeOfficer(demotee.getUniqueId());

        if (demotee.isOnline()) {
            ((Player) demotee).sendMessage(translate("&c" + getText("AlertDemotion")));
        }

        player.sendMessage(translate("&a" + getText("PlayerDemoted")));
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
