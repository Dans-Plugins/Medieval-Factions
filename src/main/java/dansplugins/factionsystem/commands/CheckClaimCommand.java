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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class CheckClaimCommand extends SubCommand {

    public CheckClaimCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{"checkclaim", "cc", LOCALE_PREFIX + "CmdCheckClaim"}, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.checkclaim";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        final String result = chunkDataAccessor.checkOwnershipAtPlayerLocation(player);

        if (result.equals("unclaimed")) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&a" + getText("LandIsUnclaimed")));
            } else {
                PlayerService.sendPlayerMessage(player, "LandIsUnclaimed", true);
            }
        } else {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("LandClaimedBy", result)));
            } else {
                PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("LandClaimedBy"))
                        .replaceAll("#player#", result), false);
            }
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