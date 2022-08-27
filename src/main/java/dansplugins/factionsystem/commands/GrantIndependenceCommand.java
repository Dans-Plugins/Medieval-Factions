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
public class GrantIndependenceCommand extends SubCommand {

    public GrantIndependenceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "GrantIndependence", "GI", LOCALE_PREFIX + "CmdGrantIndependence"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (!(checkPermissions(player, "mf.grantindependence"))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantIndependence")));
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound")
                    , Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args))
                    , true);
            return;
        }
        if (!target.isLiege(this.faction.getName())) {
            player.sendMessage(translate("&c" + getText("FactionIsNotVassal")));
            return;
        }
        target.setLiege("none");
        this.faction.removeVassal(target.getName());
        // inform all players in that faction that they are now independent
        messageFaction(target, translate("&a" + getText("AlertGrantedIndependence", faction.getName())),
                Objects.requireNonNull(messageService.getLanguage().getString("AlertGrantedIndependence"))
                        .replace("#name#", faction.getName()));
        // inform all players in players faction that a vassal was granted independence
        messageFaction(faction, translate("&a" + getText("AlertNoLongerVassalFaction", target.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AlertNoLongerVassalFaction"))
                        .replace("#name#", target.getName()));
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