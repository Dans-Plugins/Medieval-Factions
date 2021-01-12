package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand {

    public boolean claim(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.claim")) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionClaim"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        // if not officer or owner
        if (!(playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId()))) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
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
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageClaimRadius"));
            }

            ChunkManager.getInstance().radiusClaimAtLocation(depth, player, player.getLocation(), playersFaction);
        }

        DynmapManager.updateClaims();
        return true;
    }

}
