package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class InviteCommand extends SubCommand {

    public InviteCommand() {
        super(new String[]{
                "invite", LOCALE_PREFIX + "CmdInvite"
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
        final String permission = "mf.invite";
        if (!(checkPermissions(player, permission))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsageInvite")));
            return;
        }
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToInviteOthers")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                return;
            }
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (playerUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                return;
            }
        }
        if (data.isInFaction(playerUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerAlreadyInFaction")));
            return;
        }
        faction.invite(playerUUID);
        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("InvitationSent"));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(translate(
                    "&a" + getText("AlertBeenInvited", faction.getName(), faction.getName())
            ));
        }

        final long seconds = 1728000L;
        // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
        final OfflinePlayer tmp = target;
        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                faction.uninvite(playerUUID);
                if (tmp.isOnline() && tmp.getPlayer() != null) {
                    tmp.getPlayer().sendMessage(translate(
                            "&c" + getText("InvitationExpired", faction.getName())
                    ));
                }
            }
        }, seconds);
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

}
