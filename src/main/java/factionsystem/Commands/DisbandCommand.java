package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class DisbandCommand extends Command {

    public DisbandCommand() {
        super();
    }

    public boolean deleteFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.disband") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    if (player.hasPermission("mf.disband.others") || player.hasPermission("mf.admin")) {

                        String factionName = createStringFromFirstArgOnwards(args);

                        for (int i = 0; i < MedievalFactions.getInstance().factions.size(); i++) {

                            if (MedievalFactions.getInstance().factions.get(i).getName().equalsIgnoreCase(factionName)) {

                                removeFaction(i);
                                player.sendMessage(ChatColor.GREEN + factionName + " has been successfully disbanded.");
                                return true;

                            }

                        }
                        player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                        return false;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.disband.others'");
                        return false;
                    }

                }

                boolean owner = false;
                for (int i = 0; i < MedievalFactions.getInstance().factions.size(); i++) {
                    if (MedievalFactions.getInstance().factions.get(i).isOwner(player.getUniqueId())) {
                        owner = true;
                        if (MedievalFactions.getInstance().factions.get(i).getPopulation() == 1) {
                            MedievalFactions.getInstance().playersInFactionChat.remove(player.getUniqueId());
                            removeFaction(i);
                            player.sendMessage(ChatColor.GREEN + "Faction successfully disbanded.");
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You need to kick all players before you can disband your faction.");
                            return false;
                        }
                    }
                }

                if (!owner) {
                    player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.disband'");
                return false;
            }
        }
        return false;
    }

    public void removeFaction(int i) {

        // remove claimed land objects associated with this faction
        removeAllClaimedChunks(MedievalFactions.getInstance().factions.get(i).getName(), MedievalFactions.getInstance().claimedChunks);

        // remove locks associated with this faction
        removeAllLocks(MedievalFactions.getInstance().factions.get(i).getName(), MedievalFactions.getInstance().lockedBlocks);

        // remove records of alliances/wars associated with this faction
        for (Faction faction : MedievalFactions.getInstance().factions) {
            if (faction.isAlly(MedievalFactions.getInstance().factions.get(i).getName())) {
                faction.removeAlly(MedievalFactions.getInstance().factions.get(i).getName());
            }
            if (faction.isEnemy(MedievalFactions.getInstance().factions.get(i).getName())) {
                faction.removeEnemy(MedievalFactions.getInstance().factions.get(i).getName());
            }
        }

        MedievalFactions.getInstance().factions.remove(i);
    }
}
