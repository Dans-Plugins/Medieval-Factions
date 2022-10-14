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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class SetHomeCommand extends SubCommand {

    public SetHomeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "sethome", "sh", LOCALE_PREFIX + "CmdSetHome"
        }, true, true, true, false, ["mf.sethome"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (!this.chunkDataAccessor.isClaimed(player.getLocation().getChunk())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("LandIsNotClaimed"),
                "LandIsNotClaimed", 
                false
            );
            return;
        }
        ClaimedChunk chunk = this.chunkDataAccessor.getClaimedChunk(player.getLocation().getChunk());
        if (chunk == null || !chunk.getHolder().equalsIgnoreCase(this.faction.getName())) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("CannotSetFactionHomeInWilderness"),
                "CannotSetFactionHomeInWilderness", 
                false
            );
            return;
        }
        this.faction.setFactionHome(player.getLocation());
        this.playerService.sendMessage(
            player, 
            "&a" + this.getText("FactionHomeSet"),
            "FactionHomeSet", 
            false
        );
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