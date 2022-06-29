/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Faction;

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
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            if (!(checkPermissions(sender, "mf.unclaimall"))) return;
            faction = getPlayerFaction(sender);
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                return;
            }
            if (!faction.isOwner(((Player) sender).getUniqueId())) {
                sender.sendMessage(translate("&c" + getText("AlertMustBeOwnerToUseCommand")));
                return;
            }
        } else {
            if (!(checkPermissions(sender, "mf.unclaimall.others", "mf.admin"))) return;
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                sender.sendMessage(translate("&c" + getText("FactionNotFound")));
                return;
            }
        }
        // remove faction home
        faction.setFactionHome(null);
        messageFaction(faction, translate("&c" + getText("AlertFactionHomeRemoved")));

        // remove claimed chunks
        chunkDataAccessor.removeAllClaimedChunks(faction.getName());
        dynmapIntegrator.updateClaims();
        sender.sendMessage(translate("&a" + getText("AllLandUnclaimedFrom", faction.getName())));

        // remove locks associated with this faction
        persistentData.removeAllLocks(faction.getName());
    }
}