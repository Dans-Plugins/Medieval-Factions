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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class UnclaimallCommand extends SubCommand {

    public UnclaimallCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "unclaimall", "ua", LOCALE_PREFIX + "CmdUnclaimall"
        }, false, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        final Faction faction;
        if (args.length == 0) {
            // Self
            if (!(sender instanceof Player)) {
                new PlayerService().sendMessageType(sender, getText("OnlyPlayersCanUseCommand")
                        , "OnlyPlayersCanUseCommand", false);
                return;
            }
            if (!(checkPermissions(sender, "mf.unclaimall"))) return;
            faction = getPlayerFaction(sender);
            if (faction == null) {
                new PlayerService().sendMessageType(sender, "&c" + getText("AlertMustBeInFactionToUseCommand"),
                        "AlertMustBeInFactionToUseCommand", false);
                return;
            }
            if (!faction.isOwner(((Player) sender).getUniqueId())) {
                new PlayerService().sendMessageType(sender, "&c" + getText("AlertMustBeOwnerToUseCommand"),
                        "AlertMustBeOwnerToUseCommand", false);
                return;
            }
        } else {
            if (!(checkPermissions(sender, "mf.unclaimall.others", "mf.admin"))) return;
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                new PlayerService().sendMessageType(sender, "&c" + getText("FactionNotFound"),
                        Objects.requireNonNull(new MessageService().getLanguage().getString("FactionNotFound"))
                                .replaceAll("#faction#", String.join(" ", args)), true);
                return;
            }
        }
        // remove faction home
        faction.setFactionHome(null);
        messageFaction(faction, translate("&c" + getText("AlertFactionHomeRemoved"))
                , new MessageService().getLanguage().getString("AlertFactionHomeRemoved"));

        // remove claimed chunks
        chunkDataAccessor.removeAllClaimedChunks(faction.getName());
        dynmapIntegrator.updateClaims();
        new PlayerService().sendMessageType(sender, "&a" + getText("AllLandUnclaimedFrom", faction.getName())
                , Objects.requireNonNull(new MessageService().getLanguage().getString("AllLandUnclaimedFrom"))
                        .replaceAll("#name#", faction.getName()), false);

        // remove locks associated with this faction
        persistentData.removeAllLocks(faction.getName());
    }
}