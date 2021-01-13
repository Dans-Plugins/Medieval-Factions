package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ResetPowerLevelsCommand {

    public boolean resetPowerLevels(CommandSender sender) {
        if (sender.hasPermission("mf.resetpowerlevels") || sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Power level resetting...");
            resetPowerRecords();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.resetpowerlevels"));
            return false;
        }
    }

    private void resetPowerRecords() {
        // reset individual records
        System.out.println(LocaleManager.getInstance().getText("ResettingIndividualPowerRecords"));
        for (PlayerPowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()) {
            record.setPowerLevel(MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
        }
    }

}
