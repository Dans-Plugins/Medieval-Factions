package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand extends SubCommand {

    public UnlockCommand() {
        super(new String[] {
                "unlock", LOCALE_PREFIX + "CmdUnlock"
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
        final String permission = "mf.unlock";
        if (!(checkPermissions(player, permission))) return;
        if (args.length != 0 && args[0].equalsIgnoreCase("cancel")) {
            ephemeral.getUnlockingPlayers().remove(player.getUniqueId());
            player.sendMessage(translate("&c" + getText("AlertUnlockingCancelled")));
            return;
        }
        if (!ephemeral.getUnlockingPlayers().contains(player.getUniqueId())) {
            // add player to playersAboutToLockSomething list
            ephemeral.getUnlockingPlayers().add(player.getUniqueId());
        }
        ephemeral.getLockingPlayers().remove(player.getUniqueId()); // Remove from locking

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        player.sendMessage(translate("&a" + getText("RightClickUnlock")));
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
    public void unlockBlock(CommandSender sender, String[] args) {
        // check if player
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // check if has permission
            if (player.hasPermission("mf.unlock")) {

                // check if argument exists
                if (args.length > 1) {

                    // cancel unlock status if first argument is "cancel"
                    if (args[1].equalsIgnoreCase("cancel")) {
                        if (EphemeralData.getInstance().getUnlockingPlayers().contains(player.getUniqueId())) {
                            EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertUnlockingCancelled"));
                            return;
                        }
                    }
                }

                // check that player has not already invoked this command without unlocking something
                if (!EphemeralData.getInstance().getUnlockingPlayers().contains(player.getUniqueId())) {
                    // add player to playersAboutToLockSomething list
                    EphemeralData.getInstance().getUnlockingPlayers().add(player.getUniqueId());

                    EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());

                    // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("RightClickUnlock"));
                }



            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.unlock"));
            }

        }
    }

}
