package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand extends SubCommand {

    public JoinCommand() {
        super(new String[] {
                "join", LOCALE_PREFIX + "CmdJoin"
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
        final String permission = "mf.join";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageJoin")));
            return;
        }
        if (data.isInFaction(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyInFaction")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            // TODO Locale Message
            return;
        }
        faction.addMember(player.getUniqueId(), data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        faction.uninvite(player.getUniqueId());
        messageFaction(faction, translate("&a" + getText("HasJoined", player.getName(), faction.getName())));
        player.sendMessage(translate("&a" + getText("AlertJoinedFaction")));
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
    public boolean joinFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.join")) {
                if (args.length > 1) {

                    // creating name from arguments 1 to the last one
                    String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(factionName)) {
                            if (faction.isInvited(player.getUniqueId())) {

                                // join if player isn't in a faction already
                                if (!(PersistentData.getInstance().isInFaction(player.getUniqueId()))) {
                                    FactionJoinEvent event = new FactionJoinEvent(faction, player);
                                    Bukkit.getPluginManager().callEvent(event);
                                    if (event.isCancelled()) {
                                        // TODO Add a message (maybe)
                                        continue; // Added because idk why this is in a loop.
                                    }
                                    faction.addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                    faction.uninvite(player.getUniqueId());
                                    try {
                                        Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("HasJoined"), player.getName(), faction.getName()));
                                    } catch (Exception ignored) {

                                    }
                                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertJoinedFaction"));
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyInFaction"));
                                    return false;
                                }

                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotInvitedToFaction"));
                                return false;
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageJoin"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.join"));
                return false;
            }
        }
        return false;
    }

}
