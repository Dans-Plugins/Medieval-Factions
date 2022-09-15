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
                LOCALE_PREFIX + "CmdCreate", "Create"
        }, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.create";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        this.faction = getPlayerFaction(player);
        if (this.faction != null) {
            playerService.sendMessageType(player, "&c" + getText("AlreadyInFaction"),
                    "AlreadyInFaction", false);
            return;
        }

        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsageCreate"),
                    "UsageCreate", false);
            return;
        }

        final String factionName = String.join(" ", args).trim();

        final FileConfiguration config = configService.getConfig();

        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            playerService.sendMessageType(player, "&c" + getText("FactionNameTooLong"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionNameTooLong"))
                            .replace("#name#", factionName), true);
            return;
        }

        if (persistentData.getFaction(factionName) != null) {
            playerService.sendMessageType(player, "&c" + getText("FactionAlreadyExists"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionAlreadyExists"))
                            .replace("#name#", factionName), true);
            return;
        }

        this.faction = new Faction(factionName, player.getUniqueId(), configService, localeService, dynmapIntegrator, logger, persistentData, medievalFactions, playerService);

        this.faction.addMember(player.getUniqueId());

        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            persistentData.addFaction(this.faction);
            playerService.sendMessageType(player, "&a" + getText("FactionCreated"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionCreated"))
                            .replace("#name#", factionName), true);
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