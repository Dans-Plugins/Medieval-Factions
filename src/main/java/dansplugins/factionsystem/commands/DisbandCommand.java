package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DisbandCommand {

    public boolean deleteFaction(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.disband") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    if (player.hasPermission("mf.disband.others") || player.hasPermission("mf.admin")) {

                        String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                        for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {

                            if (PersistentData.getInstance().getFactions().get(i).getName().equalsIgnoreCase(factionName)) {

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
                for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                    if (PersistentData.getInstance().getFactions().get(i).isOwner(player.getUniqueId())) {
                        owner = true;
                        if (PersistentData.getInstance().getFactions().get(i).getPopulation() == 1) {
                            EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
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

    private void removeFaction(int i) {

        // remove claimed land objects associated with this faction
        ChunkManager.getInstance().removeAllClaimedChunks(PersistentData.getInstance().getFactions().get(i).getName(), PersistentData.getInstance().getClaimedChunks());

        // remove locks associated with this faction
        PersistentData.getInstance().removeAllLocks(PersistentData.getInstance().getFactions().get(i).getName());

        // remove records of alliances/wars associated with this faction
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isAlly(PersistentData.getInstance().getFactions().get(i).getName())) {
                faction.removeAlly(PersistentData.getInstance().getFactions().get(i).getName());
            }
            if (faction.isEnemy(PersistentData.getInstance().getFactions().get(i).getName())) {
                faction.removeEnemy(PersistentData.getInstance().getFactions().get(i).getName());
            }
        }

        PersistentData.getInstance().getFactions().remove(i);
    }
}
