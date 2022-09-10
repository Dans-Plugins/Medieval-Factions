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
public class TransferCommand extends SubCommand {

    public TransferCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{"transfer", LOCALE_PREFIX + "CmdTransfer"}, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final String permission = "mf.transfer";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsageTransfer")
                    , "UsageTransfer", false);
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
            playerService.sendMessageType(player, "&c" + getText("PlayerIsNotInYourFaction")
                    , "PlayerIsNotInYourFaction", false);
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            playerService.sendMessageType(player, "&c" + getText("CannotTransferToSelf")
                    , "CannotTransferToSelf", false);
            return;
        }

        if (faction.isOfficer(targetUUID)) faction.removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        faction.setOwner(targetUUID);
        playerService.sendMessageType(player, "&b" + getText("OwnerShipTransferredTo", args[0])
                , Objects.requireNonNull(messageService.getLanguage().getString("OwnerShipTransferredTo"))
                        .replace("#name#", args[0]), true);
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            playerService.sendMessageType(target.getPlayer(), "&a" + getText("OwnershipTransferred", faction.getName()),
                    Objects.requireNonNull(messageService.getLanguage().getString("'OwnershipTransferred"))
                            .replace("#name#", faction.getName()), true);
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