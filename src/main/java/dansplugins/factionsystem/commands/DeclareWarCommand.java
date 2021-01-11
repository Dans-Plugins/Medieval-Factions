package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareWarCommand {

    public void declareWar(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player declaringPlayer = (Player) sender;

            if (sender.hasPermission("mf.declarewar")) {

                Faction declaringPlayersFaction = PersistentData.getInstance().getPlayersFaction(declaringPlayer.getUniqueId());

                // faction permission check
                if (!(declaringPlayersFaction.isOwner(declaringPlayer.getUniqueId()) || declaringPlayersFaction.isOfficer(declaringPlayer.getUniqueId()))) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustOwnFactionOrBeOfficer"));
                    return;
                }

                // argument check
                if (args.length < 2) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageDeclareWar"));
                    return;
                }

                // get name of potential enemy faction and get faction reference
                String potentialEnemyFactionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                Faction potentialEnemyFaction = PersistentData.getInstance().getFaction(potentialEnemyFactionName);

                // disallow declaring war on your own faction
                if (potentialEnemyFactionName.equalsIgnoreCase(declaringPlayersFaction.getName())) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDeclareWarOnYourself"));
                    return;
                }

                // faction existence check
                if (potentialEnemyFaction == null) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    return;
                }

                // already enemy check
                if (potentialEnemyFaction.isEnemy(declaringPlayersFaction.getName())) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyAtWarWith") + potentialEnemyFactionName);
                    return;
                }

                // vassal check
                if (potentialEnemyFaction.hasLiege()) {

                    // if potential enemy faction is vassal of declarer
                    if (declaringPlayersFaction.isVassal(potentialEnemyFactionName)) {
                        declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDeclareWarOnVassal"));
                        return;
                    }

                    // if lieges aren't the same
                    if (!declaringPlayersFaction.getLiege().equalsIgnoreCase(potentialEnemyFaction.getLiege())) {

                        // get liege faction of potential enemy faction
                        Faction liegeOfPotentialEnemy = PersistentData.getInstance().getFaction(potentialEnemyFaction.getLiege());

                        // if not less than half of max cumulative power level without vassal contribution
                        if (!(liegeOfPotentialEnemy.calculateCumulativePowerLevelWithoutVassalContribution() < (liegeOfPotentialEnemy.getMaximumCumulativePowerLevel() / 2))) {
                            declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDeclareWarIfLiegeNotWeakened"));
                            return;
                        }

                    }
                    else {
                        // vassals of the same liege can declare war on each other
                    }

                } // end of vassal check

                // disallow if trying to declare war on liege
                if (declaringPlayersFaction.isLiege(potentialEnemyFactionName)) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDeclareWarOnLiege"));
                    return;
                }

                // check to make sure we're not allied with this faction
                if (declaringPlayersFaction.isAlly(potentialEnemyFactionName)) {
                    declaringPlayer.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDeclareWarOnAlly"));
                    return;
                }

                // add enemy to declarer's faction's enemyList and the enemyLists of its allies
                declaringPlayersFaction.addEnemy(potentialEnemyFactionName);

                // add declarer's faction to new enemy's enemyList
                potentialEnemyFaction.addEnemy(declaringPlayersFaction.getName());

                Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + declaringPlayersFaction.getName() + LocaleManager.getInstance().getText("HasDeclaredWarAgainst") + potentialEnemyFactionName + "!");

            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionDeclareWar"));
            }
        }
    }

}
