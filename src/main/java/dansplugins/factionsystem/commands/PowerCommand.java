/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

/**
 * @author Callum Johnson
 */
public class PowerCommand extends SubCommand {

    public PowerCommand() {
        super(new String[]{
                "power", LOCALE_PREFIX + "CmdPower"
        }, false);
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
        final String permission = "mf.power";
        if (!(checkPermissions(sender, permission))) return;
        final PowerRecord record;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            record = data.getPlayersPowerRecord(((Player) sender).getUniqueId());
            sender.sendMessage(translate("&b" +
                    getText("AlertCurrentPowerLevel", record.getPower(), record.maxPower())));
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID target = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (target == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        record = data.getPlayersPowerRecord(target);
        sender.sendMessage(translate("&b" +
                getText("CurrentPowerLevel", args[0], record.getPower(), record.maxPower())));
    }
}