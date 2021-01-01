package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand {

    public boolean claim(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.claim")) {
            player.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.claim'");
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + "You must be in a faction to use this command.");
            return false;
        }

        // if not officer or owner
        if (!(playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId()))) {
            player.sendMessage(ChatColor.RED + "You must be the owner or an officer of your faction in order to use this command.");
            return false;
        }

        if (args.length == 1) {
            ChunkManager.getInstance().claimChunkAtLocation(player, player.getLocation(), playersFaction);
        }
        else {
            int depth = -1;
            try {
                depth = Integer.parseInt(args[1]);
            } catch(Exception e) {
                player.sendMessage(ChatColor.RED + "Usage: /mf claim (depth number)");
            }

            ChunkManager.getInstance().radiusClaimAtLocation(depth, player, player.getLocation(), playersFaction);
        }

        DynmapManager.updateClaims();
        return true;
    }

}
