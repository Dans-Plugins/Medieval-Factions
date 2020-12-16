package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.StringBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class DeclareWarCommand {

    public void declareWar(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player declaringPlayer = (Player) sender;

            if (sender.hasPermission("mf.declarewar") || sender.hasPermission("mf.default")) {

                Faction declaringPlayersFaction = PersistentData.getInstance().getPlayersFaction(declaringPlayer.getUniqueId());

                // faction permission check
                if (!(declaringPlayersFaction.isOwner(declaringPlayer.getUniqueId()) || declaringPlayersFaction.isOfficer(declaringPlayer.getUniqueId()))) {
                    declaringPlayer.sendMessage(ChatColor.RED + "You have to own a faction or be an officer of a faction to use this command.");
                    return;
                }

                // argument check
                if (args.length < 2) {
                    declaringPlayer.sendMessage(ChatColor.RED + "Usage: /mf declarewar (faction-name)");
                    return;
                }

                // get name of potential enemy faction and get faction reference
                String potentialEnemyFactionName = StringBuilder.getInstance().createStringFromFirstArgOnwards(args);
                Faction potentialEnemyFaction = PersistentData.getInstance().getFaction(potentialEnemyFactionName);

                // faction existence check
                if (potentialEnemyFaction == null) {
                    declaringPlayer.sendMessage(ChatColor.RED + "That faction wasn't found!");
                    return;
                }

                // already enemy check
                if (potentialEnemyFaction.isEnemy(declaringPlayersFaction.getName())) {
                    declaringPlayer.sendMessage(ChatColor.RED + "Your faction is already at war with " + potentialEnemyFactionName);
                    return;
                }

                // vassal check
                if (potentialEnemyFaction.hasLiege()) {

                    // if potential enemy faction is vassal of declarer
                    if (declaringPlayersFaction.isVassal(potentialEnemyFactionName)) {
                        declaringPlayer.sendMessage(ChatColor.RED + "You can't declare war on your own vassal!");
                        return;
                    }

                    // if lieges aren't the same
                    if (!declaringPlayersFaction.getLiege().equalsIgnoreCase(potentialEnemyFaction.getLiege())) {

                        // get liege faction of potential enemy faction
                        Faction liegeOfPotentialEnemy = PersistentData.getInstance().getFaction(potentialEnemyFaction.getLiege());

                        // if not less than half of max cumulative power level without vassal contribution
                        if (!(liegeOfPotentialEnemy.calculateCumulativePowerLevelWithoutVassalContribution() < (liegeOfPotentialEnemy.getMaximumCumulativePowerLevel() / 2))) {
                            declaringPlayer.sendMessage(ChatColor.RED + "You can't declare war on this faction as they are a vassal unless their liege, " + liegeOfPotentialEnemy.getName() + " is weakened!");
                            return;
                        }

                    }
                    else {
                        // vassals of the same liege can declare war on each other
                    }

                } // end of vassal check

                // disallow if trying to declare war on liege
                if (declaringPlayersFaction.isLiege(potentialEnemyFactionName)) {
                    declaringPlayer.sendMessage(ChatColor.RED + "You can't declare war on your liege! Try '/mf declareindependence' instead!");
                    return;
                }

                // check to make sure we're not allied with this faction
                if (declaringPlayersFaction.isAlly(potentialEnemyFactionName)) {
                    declaringPlayer.sendMessage(ChatColor.RED + "You can't declare war on your ally!");
                    return;
                }

                // add enemy to declarer's faction's enemyList and the enemyLists of its allies
                declaringPlayersFaction.addEnemy(potentialEnemyFactionName);

                // add declarer's faction to new enemy's enemyList
                potentialEnemyFaction.addEnemy(declaringPlayersFaction.getName());

                Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + declaringPlayersFaction.getName() + " has declared war against " + potentialEnemyFactionName + "!");

            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.declarewar'");
            }
        }
    }

}
