package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.LockedBlock;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RenameCommand {

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
