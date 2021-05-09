package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GrantAccessCommand extends SubCommand {

    public GrantAccessCommand() {
        super(new String[] {
                "grantaccess", "ga", LOCALE_PREFIX + "CmdGrantAccess"
        }, true);
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
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantAccess")));
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            player.sendMessage(translate("&c" + getText("CommandCancelled")));
            return;
        }
        if (ephemeral.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyGrantingAccess")));
            return;
        }
        final UUID targetUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        ephemeral.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        player.sendMessage(translate("&a" + getText("RightClickGrantAccess", args[0])));
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

    @Deprecated
    public void grantAccess(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length > 1) {

                // if args[1] is cancel, cancel this
                if (args[1].equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("CommandCancelled"));
                    return;
                }

                // if not already granting access
                if (!EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
                    // save target name and player name in hashmap in main
                    EphemeralData.getInstance().getPlayersGrantingAccess().put(player.getUniqueId(), UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]));
                    player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("RightClickGrantAccess"), args[1]));
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyGrantingAccess"));
                }
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageGrantAccess"));
            }

        }
    }

}
