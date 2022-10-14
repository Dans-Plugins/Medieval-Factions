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

/**
 * @author Callum Johnson
 */
public class ClaimCommand extends SubCommand {

    public ClaimCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "claim", LOCALE_PREFIX + "CmdClaim"
        }, true, true, [], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if ((boolean) this.faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!this.faction.isOfficer(player.getUniqueId()) && !this.faction.isOwner(player.getUniqueId())) {
                this.playerService.sendMessage(player, "&a" + this.getText("AlertMustBeOfficerOrOwnerToClaimLand"), "AlertMustBeOfficerOrOwnerToClaimLand", false);
                return;
            }
        }

        if (args.length != 0) {
            int depth = this.getIntSafe(args[0], -1);

            if (depth <= 0) {
                this.playerService.sendMessage(player, "&a" + this.getText("UsageClaimRadius"), "UsageClaimRadius", false);
            } else {
                this.chunkDataAccessor.radiusClaimAtLocation(depth, player, player.getLocation(), this.faction);
            }
        } else {
            this.chunkDataAccessor.claimChunkAtLocation(player, player.getLocation(), this.faction);
        }
        this.dynmapIntegrator.updateClaims();
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