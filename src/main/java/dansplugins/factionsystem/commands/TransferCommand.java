package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class TransferCommand extends SubCommand {

    public TransferCommand() {
        super(new String[] {
                "transfer", LOCALE_PREFIX + "CmdTransfer"
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
        final String permission = "mf.transfer";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageTransfer")));
            return;
        }
        final UUID targetUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                return;
            }
        }
        if (!faction.isMember(targetUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotInYourFaction")));
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("CannotTransferToSelf")));
            return;
        }

        if (faction.isOfficer(targetUUID)) faction.removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        faction.setOwner(targetUUID);
        player.sendMessage(translate("&b" + getText("OwnerShipTransferredTo", args[0])));
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            target.getPlayer().sendMessage(translate("&a" + getText("OwnershipTransferred", faction.getName())));
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
    public boolean transferOwnership(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.transfer")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isOwner(player.getUniqueId())) {
                        owner = true;
                        if (args.length > 1) {
                            UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {

                                if (playerUUID.equals(player.getUniqueId())) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotTransferToSelf"));
                                    return false;
                                }

                                if (faction.isOfficer(playerUUID)) {
                                    faction.removeOfficer(playerUUID);
                                }

                                // set owner
                                faction.setOwner(playerUUID);
                                player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("OwnerShipTransferredTo"), args[1]));

                                try {
                                    Player target = getServer().getPlayer(args[1]);
                                    target.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("OwnershipTransferred"), faction.getName()));
                                }
                                catch(Exception ignored) {

                                }


                                return true;
                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotInYourFaction"));
                                return false;
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageTransfer"));
                            return false;
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.transfer"));
                return false;
            }
        }
        return false;
    }
}
