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
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        final String permission = "mf.rename";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsageRename")
                    , "UsageRename", false);
            return;
        }
        final String newName = String.join(" ", args).trim();
        final FileConfiguration config = medievalFactions.getConfig();
        if (newName.length() > config.getInt("factionMaxNameLength")) {
            playerService.sendMessageType(player, "&c" + getText("FactionNameTooLong"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionNameTooLong"))
                            .replaceAll("#name#", newName), true);
            return;
        }
        final String oldName = faction.getName();
        if (getFaction(newName) != null) {
            playerService.sendMessageType(player, "&c" + getText("FactionAlreadyExists"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionAlreadyExists"))
                            .replaceAll("#name#", newName), true);
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            logger.debug("Rename event was cancelled.");
            return;
        }

        // change name
        faction.setName(newName);
        playerService.sendMessageType(player, "&a" + getText("FactionNameChanged")
                , "FactionNameChanged", false);

        persistentData.updateFactionReferencesDueToNameChange(oldName, newName);

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        persistentData.getLocalStorageService().save();
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