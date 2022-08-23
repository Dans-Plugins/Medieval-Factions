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
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class ClaimCommand extends SubCommand {

    public ClaimCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "Claim", LOCALE_PREFIX + "CmdClaim"
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
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                    player.sendMessage(translate("&c" + getText("AlertMustBeOfficerOrOwnerToClaimLand")));
                } else {
                    PlayerService.sendPlayerMessage(player, "AlertMustBeOfficerOrOwnerToClaimLand", true);
                }
                return;
            }
        }

        if (args.length != 0) {
            int depth = getIntSafe(args[0], -1);

            if (depth <= 0) {
                if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                    player.sendMessage(translate("&c" + getText("UsageClaimRadius")));
                } else {
                    PlayerService.sendPlayerMessage(player, "UsageClaimRadius", true);
                }
            } else {
                chunkDataAccessor.radiusClaimAtLocation(depth, player, player.getLocation(), faction);
            }
        } else {
            chunkDataAccessor.claimChunkAtLocation(player, player.getLocation(), faction);
        }
        dynmapIntegrator.updateClaims();
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