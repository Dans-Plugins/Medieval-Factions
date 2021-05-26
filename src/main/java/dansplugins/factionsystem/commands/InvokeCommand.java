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

import java.util.ArrayList;

public class InvokeCommand extends SubCommand {

    public InvokeCommand() {
        super(new String[]{
                "invoke", LOCALE_PREFIX + "CmdInvoke"
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
        final String permission = "mf.invoke";
        if (!(checkPermissions(player, permission))) return;
        if (args.length < 2) {
            player.sendMessage(translate("&c" + getText("UsageInvoke")));
            return;
        }
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() != 2) {
            player.sendMessage(ChatColor.RED + getText("SingleQuotesAlliedWarring"));
            return;
        }
        final Faction allyTo = getFaction(singleQuoteArgs.get(0));
        final Faction warringFaction = getFaction(singleQuoteArgs.get(1));
        if (allyTo == null || warringFaction == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (!this.faction.isAlly(allyTo.getName())) {
            player.sendMessage(translate("&c" + getText("NotAnAlly", allyTo.getName())));
            return;
        }
        if (!this.faction.isEnemy(warringFaction.getName())) {
            player.sendMessage(translate("&c" + getText("NotAtWarWith", warringFaction.getName())));
            return;
        }
        allyTo.addEnemy(warringFaction.getName());
        warringFaction.addEnemy(allyTo.getName());

        messageFaction(allyTo, // Message ally faction
                translate("&c" + getText("AlertCalledToWar1", faction.getName(), warringFaction.getName())));

        messageFaction(warringFaction, // Message warring faction
                translate("&c" + getText("AlertCalledToWar2", faction.getName(), allyTo.getName())));

        messageFaction(this.faction, // Message player faction
                translate("&a" + getText("AlertCalledToWar3", allyTo.getName(), warringFaction.getName())));
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
    public boolean invokeAlliance(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        if (player.hasPermission("mf.invoke")) {

            Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

            // faction permission check
            if (!playersFaction.isOwner(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToInvokeAlliance"));
                return false;
            }

            // args check
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageInvoke"));
                return false;
            }

            ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

            if (singleQuoteArgs.size() != 2) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SingleQuotesAlliedWarring"));
                return false;
            }

            String nameOfAllyToInvoke = singleQuoteArgs.get(0);
            String nameOfWarringFaction = singleQuoteArgs.get(1);

            // if not allied with this faction
            if (!playersFaction.isAlly(nameOfAllyToInvoke)) {
                player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("NotAnAlly"), nameOfAllyToInvoke));
                return false;
            }

            Faction allyToInvoke = PersistentData.getInstance().getFaction(nameOfAllyToInvoke);

            if (allyToInvoke == null) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                return false;
            }

            Faction warringFaction = PersistentData.getInstance().getFaction(nameOfWarringFaction);

            // if not at war with this faction
            if (!playersFaction.isEnemy(nameOfWarringFaction)) {
                player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("NotAtWarWith"), nameOfWarringFaction));
                return false;
            }

            // if warring faction doesn't exist
            if (warringFaction == null) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                return false;
            }

            allyToInvoke.addEnemy(nameOfWarringFaction);
            warringFaction.addEnemy(nameOfAllyToInvoke);

            Messenger.getInstance().sendAllPlayersInFactionMessage(allyToInvoke, ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertCalledToWar1"), playersFaction.getName(), warringFaction.getName()));
            Messenger.getInstance().sendAllPlayersInFactionMessage(warringFaction, ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertCalledToWar2"), playersFaction.getName(), allyToInvoke.getName()));
            Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertCalledToWar3"), allyToInvoke.getName(), warringFaction.getName()));
            return true;

        } else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.invoke"));
            return false;
        }
    }

}
