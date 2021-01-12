package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class MembersCommand {

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
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionMembers"));
            }
        }
    }

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
