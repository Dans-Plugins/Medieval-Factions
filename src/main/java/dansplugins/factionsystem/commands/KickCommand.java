package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KickCommand extends SubCommand {

    public KickCommand() {
        super(new String[] {
                "kick", LOCALE_PREFIX + "CmdKick"
        }, true, true, true, false);
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
        final String permission = "mf.kick";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageKick")));
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
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("CannotKickSelf")));
            return;
        }
        if (this.faction.isOwner(targetUUID)) {
            player.sendMessage(translate("&c" + getText("CannotKickOwner")));
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            // TODO Locale Message
            return;
        }
        if (faction.isOfficer(targetUUID)) faction.removeOfficer(targetUUID); // Remove Officer (if one)
        ephemeral.getPlayersInFactionChat().remove(targetUUID);
        faction.removeMember(targetUUID, data.getPlayersPowerRecord(target.getUniqueId()).getPowerLevel());
        messageFaction(faction, translate("&c" + getText("HasBeenKickedFrom", target.getName(), faction.getName())));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(translate("&c" + getText("AlertKicked", player.getName())));
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
    public boolean kickPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.kick")) {
                if (args.length > 1) {
                    boolean owner = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                            owner = true;
                            UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
                            if (faction.isMember(playerUUID)) {
                                if (!(args[1].equalsIgnoreCase(player.getName()))) {
                                    if (!(playerUUID.equals(faction.getOwner()))) {

                                        FactionKickEvent event = new FactionKickEvent(
                                                faction,
                                                Bukkit.getPlayer(playerUUID),
                                                player
                                        );
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled()) {
                                            // TO DO Add a message (maybe).
                                            continue; // Loop mechanism.
                                        }

                                        if (faction.isOfficer(playerUUID)) {
                                            faction.removeOfficer(playerUUID);
                                        }

                                        EphemeralData.getInstance().getPlayersInFactionChat().remove(playerUUID);

                                        faction.removeMember(playerUUID, PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                        try {
                                            Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + String.format(LocaleManager.getInstance().getText("HasBeenKickedFrom"),  args[1], faction.getName()));
                                        } catch (Exception ignored) {

                                        }
                                        try {
                                            Player target = Bukkit.getServer().getPlayer(args[1]);
                                            target.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertKicked"), player.getName()));
                                        } catch (Exception ignored) {

                                        }
                                        return true;
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotKickOwner"));
                                        return false;
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotKickSelf"));
                                    return false;
                                }

                            }
                        }
                    }

                    if (!owner) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeOwner"));
                        return false;
                    }

                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageKick"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.kick"));
                return false;
            }
        }
        return false;
    }

}
