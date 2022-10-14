/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionRenameEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
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
public class RenameCommand extends SubCommand {
    private final MedievalFactions medievalFactions;
    private final Logger logger;

    public RenameCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "rename"
        }, true, true, false, true, ["mf.rename"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.medievalFactions = medievalFactions;
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageRename"),
                "UsageRename",
                false
            );
            return;
        }
        final String newName = String.join(" ", args).trim();
        final FileConfiguration config = this.medievalFactions.getConfig();
        if (newName.length() > config.getInt("factionMaxNameLength")) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("FactionNameTooLong"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNameTooLong")).replace("#name#", newName),
                true
            );
            return;
        }
        final String oldName = this.faction.getName();
        if (this.getFaction(newName) != null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("FactionAlreadyExists"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionAlreadyExists")).replace("#name#", newName),
                true
            );
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(this.faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            this.logger.debug("Rename event was cancelled.");
            return;
        }

        // change name
        this.faction.setName(newName);
        this.playerService.sendMessage(
            player,
            "&a" + this.getText("FactionNameChanged"),
            "FactionNameChanged",
            false
        );

        this.persistentData.updateFactionReferencesDueToNameChange(oldName, newName);

        // Prefix (if it was unset)
        if (this.faction.getPrefix().equalsIgnoreCase(oldName)) this.faction.setPrefix(newName);

        // Save again to overwrite current data
        this.persistentData.getLocalStorageService().save();
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