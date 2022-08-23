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
            PlayerService.sendMessageType(player, "&c" + getText("UsageDemote")
                    , "UsageDemote", false);
            return;
        }

        OfflinePlayer playerToBeDemoted = null;
        for (UUID uuid : this.faction.getMemberList()) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() == null) continue;
            if (offlinePlayer.getName().equalsIgnoreCase(args[0])) playerToBeDemoted = offlinePlayer;
        }

        if (playerToBeDemoted == null) {
            PlayerService.sendMessageType(player, "&c" + getText("PlayerByNameNotFound")
                    , Objects.requireNonNull(MessageService.getLanguage().getString("PlayerByNameNotFound"))
                            .replaceAll("#name#", args[0]), true);
            return;
        }

        if (playerToBeDemoted.getUniqueId() == player.getUniqueId()) {
            PlayerService.sendMessageType(player, "&c" + getText("CannotDemoteSelf")
                    , "CannotDemoteSelf", false);
            return;
        }

        if (!this.faction.isOfficer(playerToBeDemoted.getUniqueId())) {
            PlayerService.sendMessageType(player, "&c" + getText("PlayerIsNotOfficerOfFaction")
                    , "PlayerIsNotOfficerOfFaction", false);
            return;
        }

        faction.removeOfficer(playerToBeDemoted.getUniqueId());

        if (playerToBeDemoted.isOnline()) {
            PlayerService.sendMessageType(player, "&c" + getText("AlertDemotion")
                    , "AlertDemotion", false);
        }
        PlayerService.sendMessageType(player, "&c" + getText("PlayerDemoted")
                , Objects.requireNonNull(MessageService.getLanguage().getString("PlayerDemoted"))
                        .replaceAll("#name#", playerToBeDemoted.getName()), true);
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