package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

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
        sender.sendMessage(translate("&b&l" + getText("MembersOf", faction.getName())));
        sender.sendMessage(translate("\n&b----------\n"));
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

    @Deprecated
    public void showMembers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.members")) {
                if (args.length == 1) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isMember(player.getUniqueId())) {
                            sendFactionMembers(player, faction);
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotInFaction"));
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            sendFactionMembers(player, faction);
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNameNotRecognized"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.members"));
            }
        }
    }

    @Deprecated
    private void sendFactionMembers(Player player, Faction faction) {
        ArrayList<UUID> members = faction.getMemberList();
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("MembersOf"), faction.getName()));
        player.sendMessage("\n----------\n");
        for (UUID member : members) {
            // Is Owner
            if (member.equals(faction.getOwner())){
                player.sendMessage(ChatColor.AQUA + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(member) + "**\n");
            } else if (faction.isOfficer(member)) {
                player.sendMessage(ChatColor.AQUA + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(member) + "*\n");
            } else {
                player.sendMessage(ChatColor.AQUA + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(member) + "\n");
            }
        }
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

}
