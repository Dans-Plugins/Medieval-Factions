package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckAccessCommand extends SubCommand {

    public CheckAccessCommand() {
        super(new String[] {
            "ca", "checkaccess", LOCALE_PREFIX + "CmdCheckAccess"
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
        final String permission = "mf.checkaccess";
        if (!(checkPermissions(player, permission))) return;
        boolean cancel = false, contains = ephemeral.getPlayersCheckingAccess().contains(player.getUniqueId());
        if (args.length >= 1) cancel = args[0].equalsIgnoreCase("cancel");
        if (cancel && contains) {
            ephemeral.getPlayersCheckingAccess().remove(player.getUniqueId());
            player.sendMessage(translate("&c" + getText("Cancelled")));
        } else {
            if (contains) player.sendMessage(translate("&c" + getText("AlreadyEnteredCheckAccess")));
            else {
                ephemeral.getPlayersCheckingAccess().add(player.getUniqueId());
                player.sendMessage(translate("&c" + getText("RightClickCheckAccess")));
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

    @Deprecated
    public boolean checkAccess(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.checkaccess")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.checkaccess"));
            return false;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                EphemeralData.getInstance().getPlayersCheckingAccess().remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("Cancelled"));
                return true;
            }
            else {
                return false;
            }
        }

        if (!EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
            EphemeralData.getInstance().getPlayersCheckingAccess().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("RightClickCheckAccess"));
        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlreadyEnteredCheckAccess"));
        }

        return true;
    }

}
