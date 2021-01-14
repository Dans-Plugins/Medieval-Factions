package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class VersionCommand {

    public boolean showVersion(CommandSender sender) {
        if (sender.hasPermission("mf.version")) {
            sender.sendMessage(ChatColor.AQUA + "Medieval-Factions-" + MedievalFactions.getInstance().getVersion());
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.version"));
            return false;
        }
    }

}
