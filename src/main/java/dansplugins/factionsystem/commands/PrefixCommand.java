package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrefixCommand extends SubCommand {

    public PrefixCommand() {
        super(new String[] {
                "prefix", LOCALE_PREFIX + "CmdPrefix"
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
        final String permission = "mf.prefix";
        if (!(checkPermissions(player, permission))) return;
        final String prefix = String.join(" ", args);
        if (data.getFactions().stream().map(Faction::getPrefix)
                .anyMatch(prfix -> prfix.equalsIgnoreCase(prefix))) {
            player.sendMessage(translate("&c" + getText("PrefixTaken")));
            return;
        }
        faction.setPrefix(prefix);
        player.sendMessage(translate("&a" + getText("PrefixSet")));
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
    public boolean changePrefix(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.prefix")) {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.prefix"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsagePrefix"));
            return false;
        }

        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (faction == null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            return false;
        }

        if (!faction.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
            return false;
        }

        String newPrefix = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (isPrefixTaken(newPrefix)) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PrefixTaken"));
            return false;
        }

        faction.setPrefix(newPrefix);

        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("PrefixSet"));

        return true;
    }

    @Deprecated
    private boolean isPrefixTaken(String prefix) {
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.getPrefix().equalsIgnoreCase(prefix)) {
                return true;
            }
        }
        return false;
    }

}
