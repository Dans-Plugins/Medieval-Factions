/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionRenameEvent;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class RenameCommand extends SubCommand {

    public RenameCommand() {
        super(new String[]{
                "rename"
        }, true, true, false, true);
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
            player.sendMessage(translate("&c" + getText("UsageRename")));
            return;
        }
        final String newName = String.join(" ", args).trim();
        final FileConfiguration config = MedievalFactions.getInstance().getConfig();
        if (newName.length() > config.getInt("factionMaxNameLength")) {
            player.sendMessage(translate("&c" + getText("FactionNameTooLong")));
            return;
        }
        final String oldName = faction.getName();
        if (getFaction(newName) != null) {
            player.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            Logger.getInstance().log("Rename event was cancelled.");
            return;
        }

        // change name
        faction.setName(newName);
        player.sendMessage(translate("&a" + getText("FactionNameChanged")));

        PersistentData.getInstance().updateFactionReferencesDueToNameChange(oldName, newName);

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        PersistentData.getInstance().getLocalStorageService().save();
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