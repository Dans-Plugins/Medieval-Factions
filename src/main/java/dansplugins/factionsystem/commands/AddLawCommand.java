package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddLawCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AddLawCommand() {
        super(new String[] {
                "ADD_LAW", LOCALE_PREFIX + "CMDAddLaw", "AL"
        }, true, true, false, true);
    }

    /**
     * Method to execute the command.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.addlaw";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) { // Check if they have provided any strings beyond "Add Law".
            player.sendMessage(translate("&c" + getText("UsageAddLaw")));
            return;
        }
        // Add the law and send a success message.
        faction.addLaw(String.join(" ", args));
        player.sendMessage(translate("&a" + getText("LawAdded")));
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
    public boolean addLaw(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.addlaw")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.addlaw"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageAddLaw"));
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

        String newLaw = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
        playersFaction.addLaw(newLaw);
        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawAdded"));

        return true;
    }

}
