package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionWarEndEvent;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MakePeaceCommand extends SubCommand {

    public MakePeaceCommand() {
        super(new String[] {
            "makepeace", "mp", LOCALE_PREFIX + "CmdMakePeace"
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
        final String permission = "mf.makepeace";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageMakePeace")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (target == faction) {
            player.sendMessage(translate("&c" + getText("CannotMakePeaceWithSelf")));
            return;
        }
        if (faction.isTruceRequested(target.getName())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyRequestedPeace")));
            return;
        }
        if (!faction.isEnemy(target.getName())) {
            player.sendMessage(translate("&c" + getText("FactionNotEnemy")));
            return;
        }
        faction.requestTruce(target.getName());
        player.sendMessage(translate("&a" + getText("AttemptedPeace", target.getName())));
        messageFaction(target,
                translate("&a" + getText("HasAttemptedToMakePeaceWith", faction.getName(), target.getName())));
        if (faction.isTruceRequested(target.getName()) && target.isTruceRequested(faction.getName())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(this.faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (!warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again and they need to make peace again
                faction.removeRequestedTruce(target.getName());
                target.removeRequestedTruce(faction.getName());

                // make peace between factions
                faction.removeEnemy(target.getName());
                target.removeEnemy(faction.getName());

                // Notify
                messageServer(translate("&a" + getText("AlertNowAtPeaceWith", faction.getName(), target.getName())));
            }
        }

        // if faction was a liege, then make peace with all of their vassals as well
        if (target.isLiege()) {
            for (String vassalName : target.getVassals()) {
                faction.removeEnemy(vassalName);

                Faction vassal = getFaction(vassalName);
                vassal.removeEnemy(faction.getName());
            }
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
