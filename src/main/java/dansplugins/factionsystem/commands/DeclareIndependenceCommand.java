package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand extends SubCommand {

    public DeclareIndependenceCommand() {
        super(new String[]{
                "declareindependence", "di", LOCALE_PREFIX + "CmdDeclareIndependence"
        }, true, true, false, true);
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
        final String permission = "mf.declareindependence";
        if (!(checkPermissions(player, permission))) return;
        if (!(this.faction.hasLiege()) || this.faction.getLiege() == null) {
            player.sendMessage(translate("&c" + getText("NotAVassalOfAFaction")));
            return;
        }
        final Faction liege = getFaction(this.faction.getLiege());
        if (liege == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        // break vassal agreement.
        liege.removeVassal(this.faction.getName());
        this.faction.setLiege("none");
        // Make enemies.
        this.faction.addEnemy(liege.getName());
        liege.addEnemy(this.faction.getName());
        messageServer(translate("&c" + getText("HasDeclaredIndependence", faction.getName(), liege.getName())));
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
    public boolean declareIndependence(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.declareindependence")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.declareindependence"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!playersFaction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        if (!playersFaction.hasLiege()) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotAVassalOfAFaction"));
            return false;
        }

        Faction targetFaction = PersistentData.getInstance().getFaction(playersFaction.getLiege());

        // break vassal agreement
        targetFaction.removeVassal(playersFaction.getName());
        playersFaction.setLiege("none");

        // add enemy to declarer's faction's enemyList and the enemyLists of its allies
        playersFaction.addEnemy(targetFaction.getName());

        // add declarer's faction to new enemy's enemyList
        targetFaction.addEnemy(playersFaction.getName());

        Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("HasDeclaredIndependence"), playersFaction.getName(), targetFaction.getName()));
        return true;
    }

}
