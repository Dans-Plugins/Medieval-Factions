package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareWarCommand extends SubCommand {

    public DeclareWarCommand() {
        super(new String[]{
                "declarewar", "dw", LOCALE_PREFIX + "CmdDeclareWar"
        }, true, true, true, false);
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
        final String permission = "mf.declarewar";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageDeclareWar")));
            return;
        }
        final Faction opponent = getFaction(String.join(" ", args));
        if (opponent == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (opponent == faction) {
            player.sendMessage(translate("&c" + getText("CannotDeclareWarOnYourself")));
            return;
        }
        if (faction.isEnemy(opponent.getName())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyAtWarWith", opponent.getName())));
            return;
        }
        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getName())) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarOnVassal")));
                return;
            }
            if (!faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = getFaction(opponent.getLiege());
                if (enemyLiege.calculateCumulativePowerLevelWithoutVassalContribution() <
                        enemyLiege.getMaximumCumulativePowerLevel() / 2) {
                    player.sendMessage(translate("&c" + getText("CannotDeclareWarIfLiegeNotWeakened")));
                }
            }
        }
        if (faction.isLiege(opponent.getName())) {
            player.sendMessage(translate("&c" + getText("CannotDeclareWarOnLiege")));
            return;
        }
        if (faction.isAlly(opponent.getName())) {
            player.sendMessage(translate("&c" + getText("CannotDeclareWarOnAlly")));
            return;
        }
        // Make enemies.
        faction.addEnemy(opponent.getName());
        opponent.addEnemy(faction.getName());
        messageServer(translate("&c" + getText("HasDeclaredWarAgainst", faction.getName(), opponent.getName())));
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
                    declaringPlayer.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertAlreadyAtWarWith"), potentialEnemyFactionName));
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
                        if (!(liegeOfPotentialEnemy.calculateCumulativePowerLevelWithoutVassalContribution() <
                                (liegeOfPotentialEnemy.getMaximumCumulativePowerLevel() / 2))) {
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

                Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("HasDeclaredWarAgainst"), declaringPlayersFaction.getName(), potentialEnemyFactionName));

            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.declarewar"));
            }
        }
    }

}
