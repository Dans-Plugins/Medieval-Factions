/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class CreateCommand extends SubCommand {
    private final Logger logger;
    private final MedievalFactions medievalFactions;

    public CreateCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, MedievalFactions medievalFactions, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "create", LOCALE_PREFIX + "CmdCreate"
        }, true, ["mf.create"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        Faction playerFaction = getPlayerFaction(player);
        if (playerFaction != null) {
            this.playerService.sendMessage(player, "&c" + this.getText("AlreadyInFaction"),
                    "AlreadyInFaction", false);
            return;
        }

        if (args.length == 0) {
            this.playerService.sendMessage(player, "&c" + this.getText("UsageCreate"),
                    "UsageCreate", false);
            return;
        }

        final String factionName = String.join(" ", args).trim();

        final FileConfiguration config = this.configService.getConfig();

        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("FactionNameTooLong"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNameTooLong"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        if (this.persistentData.getFaction(factionName) != null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("FactionAlreadyExists"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionAlreadyExists"))
                    .replace("#name#", factionName), true
            );
            return;
        }

        playerFaction = new Faction(factionName, player.getUniqueId(), this.configService, this.localeService, this.dynmapIntegrator, this.logger, this.persistentData, this.medievalFactions, this.playerService);
        playerFaction.addMember(player.getUniqueId());
        FactionCreateEvent createEvent = new FactionCreateEvent(playerFaction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            this.persistentData.addFaction(playerFaction);
            this.playerService.sendMessage(
                player, 
                "&a" + getText("FactionCreated"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionCreated"))
                    .replace("#name#", factionName), true
            );
        }
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