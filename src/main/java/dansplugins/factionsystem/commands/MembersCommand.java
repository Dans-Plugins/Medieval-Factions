package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class MembersCommand extends SubCommand {

    public MembersCommand() {
        super(new String[] {
                "members", LOCALE_PREFIX + "CmdMembers"
        }, false);
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
        final String permission = "mf.members";
        if (!(checkPermissions(sender, permission))) return;
        final Faction faction;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            faction = getPlayerFaction(sender);
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                return;
            }
        } else {
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("FactionNameNotRecognized")));
                return;
            }
        }
        // send Faction Members
        sender.sendMessage(translate("&b----------\n" + getText("MembersOf", faction.getName())));
        sender.sendMessage(translate("&b----------\n"));
        faction.getMemberList().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(player -> {
                    String rank = "";
                    String color = "&a";
                    if (faction.isOfficer(player.getUniqueId())) {
                        rank = "*";
                        color = "&b";
                    }
                    if (faction.isOwner(player.getUniqueId())) {
                        rank = "**";
                        color = "&c";
                    }
                    return translate("&f" + player.getName() + color + rank);
                }).forEach(sender::sendMessage);
        sender.sendMessage(translate("&b----------\n"));
    }
}