package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimCommand extends SubCommand {

    public UnclaimCommand() {
        super(new String[] {
                "unclaim", LOCALE_PREFIX + "CmdUnclaim"
        }, true, true);
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
        final String permission = "mf.unclaim";
        if (!(checkPermissions(player, permission))) return;
        chunks.removeChunkAtPlayerLocation(player);
        dynmap.updateClaims();
        // TODO: 12/05/2021 Locale Message.
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
    public boolean unclaim(CommandSender sender) {
        if (sender.hasPermission("mf.unclaim")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    ChunkManager.getInstance().removeChunkAtPlayerLocation(player);
                    DynmapManager.getInstance().updateClaims();
                    return true;
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                    return false;
                }

            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.unclaim"));
            return false;
        }
        return false;
    }

}
