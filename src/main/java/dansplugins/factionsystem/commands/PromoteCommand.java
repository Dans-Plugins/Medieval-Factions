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

public class PromoteCommand extends SubCommand {

    public PromoteCommand() {
        super(new String[] {
                "promote", LOCALE_PREFIX + "CmdPromote"
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
        final String permission = "mf.promote";
        if (!(checkPermissions(player, permission))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsagePromote")));
            return;
        }
        final UUID targetUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (!faction.isMember(targetUUID)) {
             player.sendMessage(translate("&c" + getText("PlayerIsNotMemberOfFaction")));
             return;
        }
        if (faction.isOfficer(targetUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerAlreadyOfficer")));
            return;
        }
        if(faction.addOfficer(targetUUID)){
            player.sendMessage(translate("&a" + getText("PlayerPromoted")));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(translate("&c" + getText("PromotedToOfficer")));
            }
        } else {
            player.sendMessage(translate("&c" +
                    getText("PlayerCantBePromotedBecauseOfLimit", faction.calculateMaxOfficers())));
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
    public void promotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.promote")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.isMember(playerUUID)) {
                                        if (faction.isOfficer(playerUUID)) {
                                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerAlreadyOfficer"));
                                            return;
                                        }

                                        if(faction.addOfficer(playerUUID)){
                                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerPromoted"));

                                            try {
                                                Player target = getServer().getPlayer(args[1]);
                                                target.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PromotedToOfficer"));
                                            }
                                            catch(Exception ignored) {

                                            }
                                        } else {
                                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PlayerCantBePromotedBecauseOfLimit"), faction.calculateMaxOfficers()));
                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotMemberOfFaction"));
                                    }

                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsagePromote"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.promote"));
            }
        }
    }
}
