package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.IntStream;

public class LawsCommand extends SubCommand {

    public LawsCommand() {
        super(new String[] {
                "laws", LOCALE_PREFIX + "CmdLaws"
        }, true);
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
        final String permission = "mf.laws";
        if (!(checkPermissions(player, permission))) return;
        final Faction target;
        if (args.length == 0) {
            target = getPlayerFaction(player);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("MustBeInFaction")));
                return;
            }
            if (target.getNumLaws() == 0) {
                player.sendMessage(translate("&c" + getText("AlertNoLaws")));
                return;
            }
        } else {
            target = getFaction(String.join(" ", args));
            if (target == null) {
                player.sendMessage(translate("&c" + getText("FactionNotFound")));
                return;
            }
            if (target.getNumLaws() == 0) {
                player.sendMessage(translate("&c" + getText("FactionDoesNotHaveLaws")));
                return;
            }
        }
        player.sendMessage(translate("&b" + getText("LawsTitle", faction.getName())));
        IntStream.range(0, faction.getNumLaws())
                .mapToObj(i -> translate("&b" + (i + 1) + ". " + faction.getLaws().get(i)))
                .forEach(player::sendMessage);
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
    public void showLawsToPlayer(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.laws")) ) {

            Player player = (Player) sender;

            Faction faction = null;

            if (args.length == 1) {
                faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
            }
            else {
                String target = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                boolean exists = false;
                for (Faction f : PersistentData.getInstance().getFactions()) {
                    if (f.getName().equalsIgnoreCase(target)) {
                        faction = PersistentData.getInstance().getFaction(target);
                        exists = true;
                    }
                }
                if (!exists) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    return;
                }
            }

            if (faction != null) {

                if (faction.getNumLaws() != 0) {

                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("LawsTitle"), faction.getName()));

                    // list laws
                    int counter = 1;
                    for (String law : faction.getLaws()) {
                        player.sendMessage(ChatColor.AQUA + "" + counter + ". " + faction.getLaws().get(counter - 1));
                        counter++;
                    }

                }
                else {
                    if (args.length == 1) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNoLaws"));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionDoesNotHaveLaws"));
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));

            }

        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.laws"));
        }
    }

}
