package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WhoCommand extends SubCommand {

    public WhoCommand() {
        super(new String[]{"Who", LOCALE_PREFIX + "CmdWho"}, true);
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
        final String permission = "mf.who";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageWho")));
            return;
        }
        final UUID targetUUID = MedievalFactions.getInstance().getToolbox().getUUIDChecker().findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final Faction temp = getPlayerFaction(targetUUID);
        if (temp == null) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotInAFaction")));
            return;
        }
        Messenger.getInstance().sendFactionInfo(player, temp,
                chunks.getChunksClaimedByFaction(temp.getName(), data.getClaimedChunks()));
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