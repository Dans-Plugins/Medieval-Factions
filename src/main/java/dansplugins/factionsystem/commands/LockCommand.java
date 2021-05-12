package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockCommand extends SubCommand {

    public LockCommand() {
        super(new String[] {
                "lock", LOCALE_PREFIX + "CmdLock"
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
        final String permission = "mf.lock";
        if (!(checkPermissions(player, permission))) return;
        if (args.length >= 1 && safeEquals(false, args[0], "cancel")) {
            if (ephemeral.getLockingPlayers().remove(player.getUniqueId())) { // Remove them
                player.sendMessage(translate("&c" + getText("LockingCancelled")));
                return;
            }
        }
        ephemeral.getLockingPlayers().add(player.getUniqueId());
        ephemeral.getUnlockingPlayers().remove(player.getUniqueId());
        player.sendMessage(translate("&a" + getText("RightClickLock")));
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
    public void lockBlock(CommandSender sender, String[] args) {
        // check if player
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // check if has permission
            if (player.hasPermission("mf.lock")) {

                // check if argument exists
                if (args.length > 1) {

                    // cancel lock status if first argument is "cancel"
                    if (args[1].equalsIgnoreCase("cancel")) {
                        if (EphemeralData.getInstance().getLockingPlayers().contains(player.getUniqueId())) {
                            EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("LockingCancelled"));
                            return;
                        }
                    }
                }

                // check that player has not already invoked this command without locking something
                if (!EphemeralData.getInstance().getLockingPlayers().contains(player.getUniqueId())) {
                    // add player to playersAboutToLockSomething list
                    EphemeralData.getInstance().getLockingPlayers().add(player.getUniqueId());

                    EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());

                    // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("RightClickLock"));
                }



            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.lock"));
            }

        }
    }

}
