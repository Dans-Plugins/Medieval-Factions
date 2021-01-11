package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand {

    public boolean createFaction(CommandSender sender, String[] args) {
        // player check
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.create")) {
                // player membership check
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isMember(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlreadyInFaction"));
                        return false;
                    }
                }

                // argument check
                if (args.length > 1) {

                    // creating name from arguments 1 to the last one
                    String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    // faction existence check
                    boolean factionExists = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            factionExists = true;
                            break;
                        }
                    }

                    if (!factionExists) {

                        // actual faction creation
                        Faction temp = new Faction(name, player.getUniqueId(), MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel"));
                        PersistentData.getInstance().getFactions().add(temp);
                        PersistentData.getInstance().getFactions().get(PersistentData.getInstance().getFactions().size() - 1).addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                        player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("FactionCreated"));
                        return true;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAlreadyExists"));
                        return false;
                    }
                } else {

                    // wrong usage
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageCreate"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionCreate"));
                return false;
            }
        }
        return false;
    }
}
