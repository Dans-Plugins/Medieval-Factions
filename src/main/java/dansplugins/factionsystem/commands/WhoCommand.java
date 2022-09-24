/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class WhoCommand extends SubCommand {
    private final Messenger messenger;

    public WhoCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Messenger messenger, PlayerService playerService, MessageService messageService) {
        super(new String[]{"Who", LOCALE_PREFIX + "CmdWho"}, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
        this.messenger = messenger;
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
        final String permission = "mf.who";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessage(player, "&c" + getText("UsageWho")
                    , "UsageWho", false);
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]), true);
            return;
        }
        final Faction temp = getPlayerFaction(targetUUID);
        if (temp == null) {
            playerService.sendMessage(player, "&c" + getText("PlayerIsNotInAFaction")
                    , "PlayerIsNotInAFaction", false);
            return;
        }
        messenger.sendFactionInfo(player, temp,
                chunkDataAccessor.getChunksClaimedByFaction(temp.getName()));
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