/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.utils.extended.Messenger;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

/**
 * @author Callum Johnson
 */
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
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
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
                chunks.getChunksClaimedByFaction(temp.getName()));
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