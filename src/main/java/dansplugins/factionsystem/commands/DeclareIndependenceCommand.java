package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand extends SubCommand {

    public DeclareIndependenceCommand() {
        super(new String[]{
                "declareindependence", "di", LOCALE_PREFIX + "CmdDeclareIndependence"
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
        final String permission = "mf.declareindependence";
        if (!(checkPermissions(player, permission))) return;
        if (!(this.faction.hasLiege()) || this.faction.getLiege() == null) {
            player.sendMessage(translate("&c" + getText("NotAVassalOfAFaction")));
            return;
        }
        final Faction liege = getFaction(this.faction.getLiege());
        if (liege == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        // break vassal agreement.
        liege.removeVassal(this.faction.getName());
        this.faction.setLiege("none");
        // Make enemies.
        this.faction.addEnemy(liege.getName());
        liege.addEnemy(this.faction.getName());
        messageServer(translate("&c" + getText("HasDeclaredIndependence", faction.getName(), liege.getName())));
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
