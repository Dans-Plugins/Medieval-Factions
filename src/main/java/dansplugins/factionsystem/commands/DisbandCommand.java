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
import dansplugins.factionsystem.utils.TabCompleteTools;
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
    private final MedievalFactions medievalFactions;

    public DisbandCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService, MedievalFactions medievalFactions) {
        super(new String[]{
                "disband", LOCALE_PREFIX + "CmdDisband"
        }, false, [], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
        this.logger = logger;
        this.medievalFactions = medievalFactions;
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
            if (!this.checkPermissions(sender, "mf.disband")) return;
            if (!(sender instanceof Player)) { // ONLY Players can be in a Faction
                if (!this.configService.getBoolean("useNewLanguageFile")) {
                    sender.sendMessage(this.translate(this.getText("OnlyPlayersCanUseCommand")));
                } else {
                    this.playerService.sendMessageToConsole(sender.getServer().getConsoleSender(), "OnlyPlayersCanUseCommand", true);
                }
                return;
            }
            disband = this.getPlayerFaction(sender);
            self = true;
            if (disband.getPopulation() != 1) {
                this.playerService.sendMessage(
                    sender,
                    "&c" + this.getText("AlertMustKickAllPlayers"),
                    "AlertMustKickAllPlayers",
                    false
                );
                return;
            }
        } else {
            if (!this.checkPermissions(sender, "mf.disband.others", "mf.admin")) return;
            disband = this.getFaction(String.join(" ", args));
            self = false;
        }
        if (disband == null) {
            this.playerService.sendMessage(
                sender,
                "&c" + this.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        final int factionIndex = persistentData.getFactionIndexOf(disband);
        if (self) {
            this.playerService.sendMessage(
                sender,
                "&c" + this.getText("FactionSuccessfullyDisbanded"),
                "FactionSuccessfullyDisbanded", 
                false
            );
            this.ephemeralData.getPlayersInFactionChat().remove(((Player) sender).getUniqueId());
        } else {
            this.playerService.sendMessage(
                sender,
                "&c" + this.getText("SuccessfulDisbandment", disband.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("SuccessfulDisbandment")).replace("#faction#", disband.getName()), 
                true
            );
        }
        this.removeFaction(factionIndex, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(int i, OfflinePlayer disbandingPlayer) {

        Faction disbandingThisFaction = this.persistentData.getFactionByIndex(i);
        String nameOfFactionToRemove = disbandingThisFaction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                disbandingThisFaction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.logger.debug("Disband event was cancelled.");
            return;
        }

        // remove claimed land objects associated with this faction
        this.persistentData.getChunkDataAccessor().removeAllClaimedChunks(nameOfFactionToRemove);
        this.dynmapIntegrator.updateClaims();

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(this.persistentData.getFactionByIndex(i).getName());

        this.persistentData.removePoliticalTiesToFaction(nameOfFactionToRemove);

        this.persistentData.removeFactionByIndex(i);
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (! this.checkPermissions(sender)) return null;
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}