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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class UnclaimCommand extends SubCommand {

    public UnclaimCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "unclaim", LOCALE_PREFIX + "CmdUnclaim"
        }, true, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.unclaim";
        if (!(checkPermissions(player, permission))) return;
        final boolean isPlayerBypassing = ephemeralData.getAdminsBypassingProtections().contains(player.getUniqueId());
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId()) && !isPlayerBypassing) {
                playerService.sendMessage(player, "&c" + "You're not able to claim land at this time."
                        , "NotAbleToClaim", false);
                return;
            }
        }
        if (args.length == 0) {
            chunkDataAccessor.removeChunkAtPlayerLocation(player, faction);
            dynmapIntegrator.updateClaims();
            playerService.sendMessage(player, "&aUnclaimed your current claim."
                    , "UnClaimed", false);
            return;
        }
        int radius = getIntSafe(args[0], 1);
        if (radius <= 0) {
            radius = 1;
        }
        chunkDataAccessor.radiusUnclaimAtLocation(radius, player, faction);
        playerService.sendMessage(player, "Unclaimed radius of " + radius + " claims around you!"
                , Objects.requireNonNull(messageService.getLanguage().getString("UnClaimedRadius"))
                        .replace("#number#", String.valueOf(radius)), true);
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