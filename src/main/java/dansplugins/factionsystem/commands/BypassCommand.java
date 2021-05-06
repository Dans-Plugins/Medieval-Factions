package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public BypassCommand() {
        super(new String[] {
                "bypass", "Locale_CmdBypass"
        }, true, false, false, false);
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
        if (!checkPermissions(player, "mf.bypass", "mf.admin")) return;
        final boolean contains = ephemeral.getAdminsBypassingProtections().contains(player.getUniqueId());
        final String path = (contains ? "NoLonger" : "Now") + "BypassingProtections";
        if (contains) ephemeral.getAdminsBypassingProtections().remove(player.getUniqueId());
        else ephemeral.getAdminsBypassingProtections().add(player.getUniqueId());
        player.sendMessage(translate("&a" + getText(path)));
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
    public boolean toggleBypass(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!(player.hasPermission("mf.bypass") || player.hasPermission("mf.admin"))) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.bypass"));
            return false;
        }

        if (!EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
            EphemeralData.getInstance().getAdminsBypassingProtections().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NowBypassingProtections"));
        }
        else {
            EphemeralData.getInstance().getAdminsBypassingProtections().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NoLongerBypassingProtections"));
        }

        return true;

    }

}
