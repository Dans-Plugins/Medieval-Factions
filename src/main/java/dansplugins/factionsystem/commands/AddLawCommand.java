package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddLawCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AddLawCommand() {
        super(new String[] {
               LOCALE_PREFIX + "CMDAddLaw", "AL", "addlaw"
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

}
