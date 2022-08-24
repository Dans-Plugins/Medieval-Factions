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

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class InfoCommand extends SubCommand {
    private final Messenger messenger;

    public InfoCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Messenger messenger) {
        super(new String[]{
                "info", LOCALE_PREFIX + "CmdInfo"
        }, false, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.info";
        if (!(checkPermissions(sender, permission))) return;
        final Faction target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                new PlayerService().sendMessageType(sender, getText("OnlyPlayersCanUseCommand")
                        , "OnlyPlayersCanUseCommand", false);
                return;
            }
            target = getPlayerFaction(sender);
            if (target == null) {
                new PlayerService().sendMessageType(sender, "&c" + getText("AlertMustBeInFactionToUseCommand")
                        , "AlertMustBeInFactionToUseCommand", false);
                return;
            }
        } else {
            target = getFaction(String.join(" ", args));
            if (target == null) {
                new PlayerService().sendMessageType(sender, "&c" + getText("FactionNotFound")
                        , Objects.requireNonNull(new MessageService().getLanguage().getString("FactionNotFound"))
                                .replaceAll("#faction#", String.join(" ", args)), true);
                return;
            }
        }
        messenger.sendFactionInfo(sender, target, target.getClaimedChunks().size());
    }
}