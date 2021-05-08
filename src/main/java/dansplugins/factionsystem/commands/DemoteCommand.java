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

import java.util.Optional;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class DemoteCommand extends SubCommand {

    public DemoteCommand() {
        super(new String[]{
                "demote", LOCALE_PREFIX + "CmdDemote"
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
        final String permission = "mf.demote";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageDemote")));
            return;
        }
        OfflinePlayer demotee = null;
        for (UUID uuid : this.faction.getMemberList()) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() == null) continue;
            if (offlinePlayer.getName().equalsIgnoreCase(args[0])) demotee = offlinePlayer;
        }
        if (demotee == null) {
            player.sendMessage(translate("&c" + getText("PlayerByNameNotFound", args[0])));
            return;
        }
        if (!this.faction.isOfficer(demotee.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotOfficerOfFaction")));
            return;
        }
        faction.removeOfficer(demotee.getUniqueId());
        if (demotee.isOnline()) ((Player) demotee).sendMessage(translate("&c" + getText("AlertDemoted")));
        player.sendMessage(translate("&a" + getText("PlayerDemoted")));
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
    public void demotePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.demote")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (args.length > 1) {
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            UUID officerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (officerUUID != null && faction.isOfficer(officerUUID)) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    if (faction.removeOfficer(officerUUID)) {

                                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PlayerDemoted"));

                                        try {
                                            Player target = getServer().getPlayer(officerUUID);
                                            target.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertDemotion"));
                                        }
                                        catch(Exception ignored) {

                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotOfficerOfFaction"));
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageDemote"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.demote"));
            }
        }
    }

}
