package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveLawCommand extends SubCommand {

    public RemoveLawCommand() {
        super(new String[] {
                "removelaw", LOCALE_PREFIX + "CmdRemoveLaw"
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
        final String permission = "mf.removelaw";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageRemoveLaw")));
            return;
        }
        final int lawToRemove = getIntSafe(args[0], 0) - 1;
        if (lawToRemove < 0) {
            player.sendMessage(translate("&c" + getText("UsageRemoveLaw")));
            return;
        }
        if (faction.removeLaw(lawToRemove)) player.sendMessage(translate("&a" + getText("LawRemoved")));
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
    public void removeLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && (((Player) sender).hasPermission("mf.removelaw"))) {

            Player player = (Player) sender;

            if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        int lawToRemove = Integer.parseInt(args[1]) - 1;

                        if (playersFaction.removeLaw(lawToRemove)) {
                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawRemoved"));
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageRemoveLaw"));
                    }

                }

            } else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            }
        } else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.removelaw"));
        }

    }
}
