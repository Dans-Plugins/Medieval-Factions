package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand extends SubCommand {

    public SetHomeCommand() {
        super(new String[] {
                "sethome", "sh", LOCALE_PREFIX + "CmdSetHome"
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
        final String permission = "mf.sethome";
        if (!(checkPermissions(player, permission))) return;
        if (!chunks.isClaimed(player.getLocation().getChunk(), data.getClaimedChunks())) {
            player.sendMessage(translate("&c" + getText("LandIsNotClaimed")));
            return;
        }
        ClaimedChunk chunk = chunks.getClaimedChunk(player.getLocation().getChunk(), data.getClaimedChunks());
        if (chunk == null || !chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            player.sendMessage(translate("&c" + getText("CannotSetFactionHomeInWilderness")));
            return;
        }
        faction.setFactionHome(player.getLocation());
        player.sendMessage(translate("&a" + getText("FactionHomeSet")));
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

    public void setHome(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.sethome")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        if (ChunkManager.getInstance().isClaimed(player.getLocation().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                            ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), player.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                            if (chunk.getHolder().equalsIgnoreCase(playersFaction.getName())) {
                                playersFaction.setFactionHome(player.getLocation());
                                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("FactionHomeSet"));
                            }
                            else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotSetFactionHomeInWilderness"));
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("LandIsNotClaimed"));
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.sethome"));
            }
        }
    }
}
