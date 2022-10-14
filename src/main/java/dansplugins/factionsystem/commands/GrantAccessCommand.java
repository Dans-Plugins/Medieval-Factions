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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class GrantAccessCommand extends SubCommand {

    public GrantAccessCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "grantaccess", "ga", LOCALE_PREFIX + "CmdGrantAccess"
        }, true, [], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageGrantAccess"),
                "UsageGrantAccess",
                false
            );
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CommandCancelled"),
                "CommandCancelled",
                false
            );
            return;
        }
        if (this.ephemeralData.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("AlertAlreadyGrantingAccess"),
                "AlertAlreadyGrantingAccess",
                false
            );
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                true
            );
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotGrantAccessToSelf"),
                "CannotGrantAccessToSelf",
                false
            );
            return;
        }
        this.ephemeralData.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        this.playerService.sendMessage(
            player,
            "&a" + this.getText("RightClickGrantAccess", args[0]),
            Objects.requireNonNull(this.messageService.getLanguage().getString("RightClickGrantAccess")).replace("#name#", args[0]),
            true
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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        return TabCompleteTools.allOnlinePlayersMatching(args[0]);
    }
}