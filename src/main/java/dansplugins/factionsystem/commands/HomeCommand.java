/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.extended.Scheduler;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class HomeCommand extends SubCommand {
    private final Scheduler scheduler;

    public HomeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Scheduler scheduler) {
        super(new String[]{
                "home", LOCALE_PREFIX + "CmdHome"
        }, true, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
        this.scheduler = scheduler;
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
        if (!(checkPermissions(player, "mf.home"))) return;
        if (faction.getFactionHome() == null) {
            PlayerService.sendMessageType(player, "&c" + getText("FactionHomeNotSetYet")
                    , "FactionHomeNotSetYet", false);
            return;
        }
        final Chunk home_chunk;
        if (!chunkDataAccessor.isClaimed(home_chunk = faction.getFactionHome().getChunk())) {
            PlayerService.sendMessageType(player, "&c" + getText("HomeIsInUnclaimedChunk")
                    , "HomeIsInUnclaimedChunk", false);
            return;
        }
        ClaimedChunk chunk = chunkDataAccessor.getClaimedChunk(home_chunk);
        if (chunk == null || chunk.getHolder() == null) {
            PlayerService.sendMessageType(player, "&c" + getText("HomeIsInUnclaimedChunk")
                    , "HomeIsInUnclaimedChunk", false);
            return;
        }
        if (!chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            PlayerService.sendMessageType(player, "&c" + getText("HomeClaimedByAnotherFaction")
                    , "HomeClaimedByAnotherFaction", false);
            return;
        }
        scheduler.scheduleTeleport(player, faction.getFactionHome());
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