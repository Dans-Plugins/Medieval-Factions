/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionDisbandEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class DisbandCommand extends SubCommand {
    private final Logger logger;

    public DisbandCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger) {
        super(new String[]{
                "disband", LOCALE_PREFIX + "CmdDisband"
        }, false, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
        this.logger = logger;
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
        final Faction disband;
        final boolean self;
        if (args.length == 0) {
            if (!checkPermissions(sender, "mf.disband")) return;
            if (!(sender instanceof Player)) { // ONLY Players can be in a Faction
                if (!new MedievalFactions().USE_NEW_LANGUAGE_FILE) {
                    sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                } else {
                    new PlayerService().sendConsoleMessage(sender.getServer().getConsoleSender(), "OnlyPlayersCanUseCommand", true);
                }
                return;
            }
            disband = getPlayerFaction(sender);
            self = true;
            if (disband.getPopulation() != 1) {
                new PlayerService().sendMessageType(sender, "&c" + getText("AlertMustKickAllPlayers")
                        , "AlertMustKickAllPlayers", false);
                return;
            }
        } else {
            if (!checkPermissions(sender, "mf.disband.others", "mf.admin")) return;
            disband = getFaction(String.join(" ", args));
            self = false;
        }
        if (disband == null) {
            new PlayerService().sendMessageType(sender, "&c" + getText("FactionNotFound")
                    , Objects.requireNonNull(new MessageService().getLanguage().getString("FactionNotFound"))
                            .replaceAll("#faction#", String.join(" ", args)), true);
            return;
        }
        final int factionIndex = persistentData.getFactionIndexOf(disband);
        if (self) {
            new PlayerService().sendMessageType(sender, "&c" + getText("FactionSuccessfullyDisbanded")
                    , "FactionSuccessfullyDisbanded", false);
            ephemeralData.getPlayersInFactionChat().remove(((Player) sender).getUniqueId());
        } else {
            new PlayerService().sendMessageType(sender, "&c" + getText("SuccessfulDisbandment", disband.getName())
                    , Objects.requireNonNull(new MessageService().getLanguage().getString("SuccessfulDisbandment")).replaceAll("#faction#", disband.getName()), true);
        }
        removeFaction(factionIndex, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(int i, OfflinePlayer disbandingPlayer) {

        Faction disbandingThisFaction = persistentData.getFactionByIndex(i);
        String nameOfFactionToRemove = disbandingThisFaction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                disbandingThisFaction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            logger.debug("Disband event was cancelled.");
            return;
        }

        // remove claimed land objects associated with this faction
        persistentData.getChunkDataAccessor().removeAllClaimedChunks(nameOfFactionToRemove);
        dynmapIntegrator.updateClaims();

        // remove locks associated with this faction
        persistentData.removeAllLocks(persistentData.getFactionByIndex(i).getName());

        persistentData.removePoliticalTiesToFaction(nameOfFactionToRemove);

        persistentData.removeFactionByIndex(i);
    }
}