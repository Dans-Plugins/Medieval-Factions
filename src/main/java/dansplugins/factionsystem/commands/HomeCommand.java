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
import dansplugins.factionsystem.services.MessageService;
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

    public HomeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Scheduler scheduler, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "home", LOCALE_PREFIX + "CmdHome"
        }, true, true, ["mf.home"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (this.faction.getFactionHome() == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("FactionHomeNotSetYet"),
                "FactionHomeNotSetYet", 
                false
            );
            return;
        }
        final Chunk home_chunk;
        if (!this.chunkDataAccessor.isClaimed(home_chunk = this.faction.getFactionHome().getChunk())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("HomeIsInUnclaimedChunk"),
                "HomeIsInUnclaimedChunk", 
                false
            );
            return;
        }
        ClaimedChunk chunk = this.chunkDataAccessor.getClaimedChunk(home_chunk);
        if (chunk == null || chunk.getHolder() == null) {
            this.playerService.sendMessage(
                player, "&c" + this.getText("HomeIsInUnclaimedChunk"),
                "HomeIsInUnclaimedChunk", 
                false
            );
            return;
        }
        if (!chunk.getHolder().equalsIgnoreCase(this.faction.getName())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("HomeClaimedByAnotherFaction"),
                "HomeClaimedByAnotherFaction", 
                false
            );
            return;
        }
        this.scheduler.scheduleTeleport(player, this.faction.getFactionHome());
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