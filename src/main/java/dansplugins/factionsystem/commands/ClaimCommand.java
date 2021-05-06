package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.DynmapManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand extends SubCommand {

    public ClaimCommand() {
        super(new String[] {
                "Claim", LOCALE_PREFIX + "CmdClaim"
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
        if (args.length != 0) {
            int depth = getIntSafe(args[0], -1);
            if (depth <= 0) player.sendMessage(translate("&c" + getText("UsageClaimRadius")));
            else chunks.radiusClaimAtLocation(depth, player, player.getLocation(), faction);
        } else chunks.claimChunkAtLocation(player, player.getLocation(), faction);
        dynmap.updateClaims();
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
    public boolean claim(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.claim")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.claim"));
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

        DynmapManager.getInstance().updateClaims();
        return true;
    }

}
