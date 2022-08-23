/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
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

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class DemoteCommand extends SubCommand {

    public DemoteCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "demote", LOCALE_PREFIX + "CmdDemote"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
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
        final String permission = "mf.demote";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("UsageDemote")));
            } else {
                PlayerService.sendPlayerMessage(player, "UsageDemote", true);
            }
            return;
        }

        OfflinePlayer playerToBeDemoted = null;
        for (UUID uuid : this.faction.getMemberList()) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() == null) continue;
            if (offlinePlayer.getName().equalsIgnoreCase(args[0])) playerToBeDemoted = offlinePlayer;
        }

        if (playerToBeDemoted == null) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("PlayerByNameNotFound", args[0])));
            } else {
                PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("PlayerByNameNotFound"))
                        .replaceAll("#name#", args[0]), false);
            }
            return;
        }

        if (playerToBeDemoted.getUniqueId() == player.getUniqueId()) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDemoteSelf")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDemoteSelf", true);
            }
            return;
        }

        if (!this.faction.isOfficer(playerToBeDemoted.getUniqueId())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("PlayerIsNotOfficerOfFaction")));
            } else {
                PlayerService.sendPlayerMessage(player, "PlayerIsNotOfficerOfFaction", true);
            }
            return;
        }

        faction.removeOfficer(playerToBeDemoted.getUniqueId());

        if (playerToBeDemoted.isOnline()) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                ((Player) playerToBeDemoted).sendMessage(translate("&c" + getText("AlertDemotion")));
            } else {
                PlayerService.sendPlayerMessage(player, "AlertDemotion", true);
            }
        }
        if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
            player.sendMessage(translate("&a" + getText("PlayerDemoted")));
        } else {
            PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("PlayerDemoted"))
                    .replaceAll("#name#", playerToBeDemoted.getName()), false);
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