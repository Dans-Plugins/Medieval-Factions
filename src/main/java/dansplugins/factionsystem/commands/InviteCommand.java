/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import static org.bukkit.Bukkit.getServer;

import java.util.UUID;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dansplugins.factionsystem.commands.abs.SubCommand;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

/**
 * @author Callum Johnson
 */
public class InviteCommand extends SubCommand {
    private final MedievalFactions medievalFactions;

    public InviteCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions) {
        super(new String[]{
                "invite", LOCALE_PREFIX + "CmdInvite"
        }, true, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
        this.medievalFactions = medievalFactions;
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
        final String permission = "mf.invite";
        if (!(checkPermissions(player, permission))) return;
        if (args.length <= 0) {
            player.sendMessage(translate("&c" + getText("UsageInvite")));
            return;
        }
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToInviteOthers")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                return;
            }
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (playerUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                return;
            }
        }
        if (persistentData.isInFaction(playerUUID)) {
            player.sendMessage(translate("&c" + getText("PlayerAlreadyInFaction")));
            return;
        }
        faction.invite(playerUUID);
        player.sendMessage(ChatColor.GREEN + localeService.get("InvitationSent"));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(translate(
                    "&a" + getText("AlertBeenInvited", faction.getName(), faction.getName())
            ));
        }

        final long seconds = 1728000L;
        // make invitation expire in 24 hours, if server restarts it also expires since invites aren't saved
        final OfflinePlayer tmp = target;
        getServer().getScheduler().runTaskLater(medievalFactions, () -> {
            faction.uninvite(playerUUID);
            if (tmp.isOnline() && tmp.getPlayer() != null) {
                tmp.getPlayer().sendMessage(translate(
                        "&c" + getText("InvitationExpired", faction.getName())
                ));
            }
        }, seconds);
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
}