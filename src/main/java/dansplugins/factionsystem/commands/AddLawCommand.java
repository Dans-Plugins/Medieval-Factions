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
                "ADD_LAW", "Locale_CMDAddLaw", "AL"
        });
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
        if (!isPlayer(sender)) { // Is the CommandSender a player?
            sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
            return;
        }
        final Player player = (Player) sender; // Cast to a Player.
        final String permission = "mf.addlaw";
        if (!player.hasPermission(permission)) { // Does the player have permission to add a law?
            player.sendMessage(translate("&c" + getText("PermissionNeeded", permission)));
            return;
        }
        if (args.length == 0) { // Check if they have provided any strings beyond "Add Law".
            player.sendMessage(translate("&c" + getText("UsageAddLaw")));
            return;
        }
        final Faction playersFaction = getPlayerFaction(player); // Obtain the Player's Faction.
        if (playersFaction == null) { // If the Faction is null, they're not in a Faction.
            player.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
            return;
        }
        if (!playersFaction.isOwner(player.getUniqueId())) { // They need to be the Owner to do this.
            player.sendMessage(translate("&c" + getText("AlertMustBeOwnerToUseCommand")));
            return;
        }
        // Add the law and send a success message.
        playersFaction.addLaw(String.join(" ", args));
        player.sendMessage(translate("&a" + getText("LawAdded")));
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
