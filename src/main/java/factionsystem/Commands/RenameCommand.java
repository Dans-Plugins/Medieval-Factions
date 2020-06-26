package factionsystem.Commands;

import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Utility.UtilityFunctions.*;

public class RenameCommand {

    Main main = null;

    public RenameCommand(Main plugin) {
        main = plugin;
    }

    public void renameFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mf.rename") || player.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String oldName = getPlayersFaction(player.getName(), main.factions).getName();
                    String newName = createStringFromFirstArgOnwards(args);

                    // existence check
                    for (Faction faction : main.factions) {
                        if (faction.getName().equalsIgnoreCase(newName)) {
                            player.sendMessage(ChatColor.RED + "That name is already taken!");
                            return;
                        }
                    }

                    if (isInFaction(player.getName(), main.factions)) {
                        Faction playersFaction = getPlayersFaction(player.getName(), main.factions);
                        if (playersFaction.isOwner(player.getName())) {

                            // change name
                            playersFaction.changeName(newName);
                            player.sendMessage(ChatColor.GREEN + "Faction name changed!");

                            // save faction and faction names
                            playersFaction.save(main.factions);
                            main.saveFactionNames();

                            // change holder of claimed chunks
                            for (ClaimedChunk chunk : main.claimedChunks) {
                                if (chunk.getHolder().equalsIgnoreCase(oldName)) {
                                    chunk.setHolder(newName);
                                }
                            }

                            // save claimed chunks
                            main.saveClaimedChunks();
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You are not the owner of this faction!");
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf rename (newName)");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! You need to have the permission 'mf.rename' to use this command.");
            }
        }
    }
}
