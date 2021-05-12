package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionRenameEvent;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.LockedBlock;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
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
        final String newName = String.join(" ", args);
        final String oldName = faction.getName();
        if (getFaction(newName) != null) {
            player.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            // TODO Locale Message.
            return;
        }

        // change name
        faction.setName(newName);
        player.sendMessage(translate("&a" + getText("FactionNameChanged")));

        // Change Ally/Enemy/Vassal/Leige references
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

    @Deprecated
    public boolean renameFaction(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.rename")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.rename"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageRename"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        String oldName = playersFaction.getName();
        String newName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (!playersFaction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotTheOwnerOfThisFaction"));
            return false;
        }

        if (PersistentData.getInstance().getFaction(newName) != null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAlreadyExists"));
            return false;
        }

        FactionRenameEvent renameEvent = new FactionRenameEvent(playersFaction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            // TODO Add a message here (maybe).
            return false;
        }

        // change name
        playersFaction.setName(newName);
        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("FactionNameChanged"));

        // rename alliance, enemy, liege and vassal records
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isAlly(oldName)) {
                faction.removeAlly(oldName);
                faction.addAlly(newName);
            }
            if (faction.isEnemy(oldName)) {
                faction.removeEnemy(oldName);
                faction.addEnemy(newName);
            }
            if (faction.isLiege(oldName)) {
                faction.setLiege(newName);
            }
            if (faction.isVassal(oldName)) {
                faction.removeVassal(oldName);
                faction.addVassal(newName);
            }
        }

        // rename claimed chunk records
        for (ClaimedChunk claimedChunk : PersistentData.getInstance().getClaimedChunks()) {
            if (claimedChunk.getHolder().equalsIgnoreCase(oldName)) {
                claimedChunk.setHolder(newName);
            }
        }

        // rename locked block records
        for (LockedBlock lockedBlock : PersistentData.getInstance().getLockedBlocks()) {
            if (lockedBlock.getFactionName().equalsIgnoreCase(oldName)) {
                lockedBlock.setFaction(newName);
            }
        }

        // if prefix previously unset
        if (playersFaction.getPrefix().equalsIgnoreCase(oldName)) {
            playersFaction.setPrefix(newName);
        }

        // Save again to overwrite current data
        StorageManager.getInstance().save();
        return true;
    }
}
