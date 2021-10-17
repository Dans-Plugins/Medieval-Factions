package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.objects.IFaction;
import org.bukkit.Bukkit;
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
        final IFaction opponent = getFaction(String.join(" ", args));
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
                final IFaction enemyLiege = getFaction(opponent.getLiege());
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
        if (MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality") && ((boolean) opponent.getFlags().getFlag("neutral"))) {
            player.sendMessage(translate("&c" + getText("CannotDeclareWarOnNeutralFaction")));
            return;
        }
        if (MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality") && ((boolean) faction.getFlags().getFlag("neutral"))) {
            player.sendMessage(translate("&c" + getText("CannotDeclareWarIfNeutralFaction")));
            return;
        }
        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            messageServer(translate("&c" + getText("HasDeclaredWarAgainst", faction.getName(), opponent.getName())));
        }
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

}
