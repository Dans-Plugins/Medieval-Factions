package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class GrantIndependenceCommand extends SubCommand {

    public GrantIndependenceCommand() {
        super(new String[]{
                "GrantIndependence", "GI", LOCALE_PREFIX + "CmdGrantIndependence"
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
        if (!(checkPermissions(player, "mf.grantindependence"))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantIndependence")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!target.isLiege(this.faction.getName())) {
            player.sendMessage(translate("&c" + getText("FactionIsNotVassal")));
            return;
        }
        target.setLiege("none");
        this.faction.removeVassal(target.getName());
        // inform all players in that faction that they are now independent
        messageFaction(target, translate("&a" + getText("AlertGrantedIndependence", faction.getName())));
        // inform all players in players faction that a vassal was granted independence
        messageFaction(faction, translate("&a" + getText("AlertNoLongerVassalFaction", target.getName())));
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