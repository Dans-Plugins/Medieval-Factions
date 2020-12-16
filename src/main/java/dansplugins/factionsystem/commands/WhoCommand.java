package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WhoCommand {

    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                    Faction faction = PersistentData.getInstance().getPlayersFaction(UUIDChecker.getInstance().findUUIDBasedOnPlayerName(name));
                    if (faction != null) {
                        Messenger.getInstance().sendFactionInfo(player, faction, ChunkManager.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "That player isn't in a faction.");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf who (player-name)");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.who'");
            }
        }
    }

}
