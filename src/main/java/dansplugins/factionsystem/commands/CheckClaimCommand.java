package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckClaimCommand extends SubCommand {

    public CheckClaimCommand() {
        super(new String[] {
                "checkclaim", "cc", LOCALE_PREFIX + "CmdCheckClaim"
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
        final String permission = "mf.checkclaim";
        if (!(checkPermissions(player, permission))) return;
        final String result = chunks.checkOwnershipAtPlayerLocation(player);
        if (result.equals("unclaimed")) player.sendMessage(translate("&a" + getText("LandIsUnclaimed")));
        else player.sendMessage(translate("&c" + getText("LandClaimedBy", result)));
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
    public boolean showClaim(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.checkclaim")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.checkclaim"));
            return false;
        }

        String result = ChunkManager.getInstance().checkOwnershipAtPlayerLocation(player);

        if (result.equalsIgnoreCase("unclaimed")) {
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LandIsUnclaimed"));
            return true;
        }
        else {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("LandClaimedBy"), result));
            return false;
        }

    }

}
