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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class UnclaimCommand extends SubCommand {

    public UnclaimCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "unclaim", LOCALE_PREFIX + "CmdUnclaim"
        }, true, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
                player.sendMessage(translate("&c" + "You're not able to claim land at this time."));
                return;
            }
        }
        if (args.length == 0) {
            chunkDataAccessor.removeChunkAtPlayerLocation(player, faction);
            dynmapIntegrator.updateClaims();
            player.sendMessage("Unclaimed your current claim.");
            return;
        }
        // https://github.com/dmccoystephenson/Medieval-Factions/issues/836
        int radius = getIntSafe(args[0], 1);
        if (radius <= 0) {
            radius = 1;
            player.sendMessage("Your radius wasn't properly recognised, defaulting to 1.");
        }
        chunkDataAccessor.radiusUnclaimAtLocation(radius, player, faction);
        player.sendMessage("Unclaimed radius of " + radius + " claims around you!");
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