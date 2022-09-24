/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class KickCommand extends SubCommand {
    private final Logger logger;

    public KickCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "kick", LOCALE_PREFIX + "CmdKick"
        }, true, true, true, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.logger = logger;
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
        final String permission = "mf.kick";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessage(player, "&c" + getText("UsageKick")
                    , "UsageKick", false);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
                return;
            }
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            playerService.sendMessage(player, "&c" + getText("CannotKickSelf")
                    , "CannotKickSelf", false);
            return;
        }
        if (this.faction.isOwner(targetUUID)) {
            playerService.sendMessage(player, "&c" + getText("CannotKickOwner")
                    , "CannotKickOwner", false);
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            logger.debug("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        ephemeralData.getPlayersInFactionChat().remove(targetUUID);
        faction.removeMember(targetUUID);
        messageFaction(faction, "&c" + getText("HasBeenKickedFrom", target.getName(), faction.getName()),
                Objects.requireNonNull(messageService.getLanguage().getString("HasBeenKickedFrom"))
                        .replace("#name#", args[0])
                        .replace("#faction#", faction.getName()));
        if (target.isOnline() && target.getPlayer() != null) {
            playerService.sendMessage(player, "&c" + getText("AlertKicked", player.getName())
                    , Objects.requireNonNull(messageService.getLanguage().getString("AlertKicked"))
                            .replace("#name#", player.getName()), true);
        }
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