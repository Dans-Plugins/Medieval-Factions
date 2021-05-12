package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionLeaveEvent;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand extends SubCommand {

    public LeaveCommand() {
        super(new String[]{"leave", LOCALE_PREFIX + "CmdLeave"}, true, true, false, false);
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
        final String permission = "mf.leave";
        if (!(checkPermissions(player, permission))) return;
        final boolean isOwner = this.faction.isOwner(player.getUniqueId());
        if (isOwner) {
            new DisbandCommand().execute((CommandSender) player, args, key); // Disband the Faction.
            return;
        }
        FactionLeaveEvent leaveEvent = new FactionLeaveEvent(faction, player);
        Bukkit.getPluginManager().callEvent(leaveEvent);
        if (leaveEvent.isCancelled()) {
            // TODO Locale Message
            return;
        }

        if (faction.isOfficer(player.getUniqueId())) faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        ephemeral.getPlayersInFactionChat().remove(player.getUniqueId()); // Remove from Faction Chat.
        faction.removeMember(player.getUniqueId(), data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        player.sendMessage(translate("&b" + getText("AlertLeftFaction")));
        messageFaction(faction, translate("&a" + player.getName() + " has left " + faction.getName()));
        // TODO Edit this message.

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
    public boolean leaveFaction(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.leave")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                        if (PersistentData.getInstance().getFactions().get(i).isMember(player.getUniqueId())) {
                            if (PersistentData.getInstance().getFactions().get(i).isOwner(player.getUniqueId())) {
                                // is faction empty?
                                if (PersistentData.getInstance().getFactions().get(i).getPopulation() == 1) {
                                    // able to leave
                                    FactionLeaveEvent event = new FactionLeaveEvent(PersistentData.getInstance().getFactions().get(i), player);
                                    Bukkit.getPluginManager().callEvent(event);
                                    if (event.isCancelled()) {
                                        // TODO Add a message (maybe).
                                        continue; // Added because of loop mechanism.
                                    }

                                    if (PersistentData.getInstance().getFactions().get(i).isOfficer(player.getUniqueId())) {
                                        PersistentData.getInstance().getFactions().get(i).removeOfficer(player.getUniqueId());
                                    }

                                    // remove records of alliances/wars/vassals/lieges associated with this faction
                                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                                        if (faction.isAlly(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeAlly(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (faction.isEnemy(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeEnemy(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (faction.isVassal(PersistentData.getInstance().getFactions().get(i).getName())) {
                                            faction.removeVassal(PersistentData.getInstance().getFactions().get(i).getName());
                                        }
                                        if (PersistentData.getInstance().getFactions().get(i).isLiege(faction.getName())) {
                                            PersistentData.getInstance().getFactions().get(i).setLiege("none");
                                        }
                                    }

                                    EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());

                                    // remove claimed land objects associated with this faction
                                    ChunkManager.getInstance().removeAllClaimedChunks(PersistentData.getInstance().getFactions().get(i).getName(), PersistentData.getInstance().getClaimedChunks());
                                    DynmapManager.getInstance().updateClaims();

                                    PersistentData.getInstance().getFactions().get(i).removeMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                    PersistentData.getInstance().getFactions().remove(i);
                                    player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertLeftFactionAndItGotDeleted"));

                                    return true;
                                } else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustTransferOwnership"));
                                    return false;
                                }
                            } else {
                                // able to leave

                                if (PersistentData.getInstance().getFactions().get(i).isOfficer(player.getUniqueId())) {
                                    PersistentData.getInstance().getFactions().get(i).removeOfficer(player.getUniqueId());
                                }

                                if (EphemeralData.getInstance().getPlayersInFactionChat().contains(player.getUniqueId())) {
                                    EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
                                }

                                PersistentData.getInstance().getFactions().get(i).removeMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertLeftFaction"));
                                try {
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(PersistentData.getInstance().getFactions().get(i), ChatColor.GREEN + player.getName() + " has left " + PersistentData.getInstance().getFactions().get(i).getName());
                                } catch (Exception ignored) {

                                }
                                return true;
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                }
            } else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.leave"));
                return false;
            }
        }
        return false;
    }
}
