/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
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
public class PromoteCommand extends SubCommand {

    public PromoteCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "promote", LOCALE_PREFIX + "CmdPromote"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final String permission = "mf.promote";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsagePromote")
                    , "UsagePromote", false);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            playerService.sendMessageType(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                playerService.sendMessageType(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
                return;
            }
        }
        if (!faction.isMember(targetUUID)) {
            playerService.sendMessageType(player, "&c" + getText("PlayerIsNotMemberOfFaction")
                    , "PlayerIsNotMemberOfFaction", false);
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            playerService.sendMessageType(player, "&c" + getText("PlayerAlreadyOfficer")
                    , "PlayerAlreadyOfficer", false);
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            playerService.sendMessageType(player, "&c" + getText("CannotPromoteSelf")
                    , "CannotPromoteSelf", false);
            return;
        }
        if (faction.addOfficer(targetUUID)) {
            playerService.sendMessageType(player, "&a" + getText("PlayerPromoted")
                    , "PlayerPromoted", false);
            if (target.isOnline() && target.getPlayer() != null) {
                playerService.sendMessageType(target.getPlayer(), "&a" + getText("PromotedToOfficer")
                        , "PromotedToOfficer", false);
            }
        } else {
            playerService.sendMessageType(player, "&c" +
                            getText("PlayerCantBePromotedBecauseOfLimit", faction.calculateMaxOfficers())
                    , Objects.requireNonNull(messageService.getLanguage().getString("PlayerCantBePromotedBecauseOfLimit"))
                            .replace("#number#", String.valueOf(faction.calculateMaxOfficers())), true);
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