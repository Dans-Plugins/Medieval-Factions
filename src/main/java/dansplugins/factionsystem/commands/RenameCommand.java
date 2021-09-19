package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionRenameEvent;
import dansplugins.factionsystem.managers.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RenameCommand extends SubCommand {

    public RenameCommand() {
        super(new String[] {
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
            // TODO: add locale message
            return;
        }

        // change name
        faction.setName(newName);
        player.sendMessage(translate("&a" + getText("FactionNameChanged")));

        // Change Ally/Enemy/Vassal/Liege references
        data.getFactions().forEach(fac -> fac.updateData(oldName, newName));

        // Change Claims
        data.getClaimedChunks().stream().filter(cc -> cc.getHolder().equalsIgnoreCase(oldName))
                .forEach(cc -> cc.setHolder(newName));

        // Locked Blocks
        data.getLockedBlocks().stream().filter(lb -> lb.getFactionName().equalsIgnoreCase(oldName))
                .forEach(lb -> lb.setFaction(newName));

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        StorageManager.getInstance().save();
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
