package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand extends Command {

    public HelpCommand() {
        super();
    }

    // Each page of the help command should have a title and nine commands. This is for ease of use.
    public boolean sendHelpMessage(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
            if (args.length == 1 || args.length == 0) {
                sendPageOne(sender);
            }

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("1")) {
                    sendPageOne(sender);
                }
                if (args[1].equalsIgnoreCase("2")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 2/6 == " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf kick - Kick a player from your faction. " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf transfer - Transfer ownership of your faction to another player.\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf disband - Disband your faction (must be owner)." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf declarewar - Declare war against another faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf makepeace - Send peace offer to another faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf claim - Claim land for your faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf unclaim - Unclaim land for your faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf unclaimall - Unclaim all land for your faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf checkclaim - Check if land is claimed." + "\n");
                }
                if (args[1].equalsIgnoreCase("3")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 3/6 == " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf autoclaim - Toggle auto claim, making land claiming easier." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf promote - Promote a player to officer status." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf demote - Demote an officer to member status." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf power - Check your power level." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf sethome - Set your faction home." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf home - Teleport to your faction home." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf who - View the faction info a specific player." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf ally - Attempt to ally with a faction." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf breakalliance - Break an alliance with a faction." + "\n");
                }
                if (args[1].equalsIgnoreCase("4")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 4/6 == " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf rename - Rename your faction" + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf lock - Lock a chest or door." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf unlock Unlock a chest or door." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf grantaccess - Grant someone access to a locked block.");
                    sender.sendMessage(ChatColor.AQUA + "/mf checkaccess - Check who has access to a locked block.");
                    sender.sendMessage(ChatColor.AQUA + "/mf revokeaccess - Revoke someone's access to a locked block.");
                    sender.sendMessage(ChatColor.AQUA + "/mf laws - View the laws of your faction.");
                    sender.sendMessage(ChatColor.AQUA + "/mf addlaw - Add a law to your faction.");
                    sender.sendMessage(ChatColor.AQUA + "/mf removelaw - Remove a law from your faction.");
                }
                if (args[1].equalsIgnoreCase("5")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 5/6 == " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf editlaw - Edit an already existing law in your faction.");
                    sender.sendMessage(ChatColor.AQUA + "/mf chat - Toggle faction chat.");
                    sender.sendMessage(ChatColor.AQUA + "/mf vassalize - Offer to vassalize a faction.");
                    sender.sendMessage(ChatColor.AQUA + "/mf swearfealty - Swear fealty to a faction.");
                    sender.sendMessage(ChatColor.AQUA + "/mf declareindependence - Declare independence from your liege.");
                    sender.sendMessage(ChatColor.AQUA + "/mf grantindependence - Grant a vassal vaction independence.");
                    sender.sendMessage(ChatColor.AQUA + "/mf gate create (<optional>name)");
                    sender.sendMessage(ChatColor.AQUA + "/mf gate name (<optional>name)");
                    sender.sendMessage(ChatColor.AQUA + "/mf gate list");
                    sender.sendMessage(ChatColor.AQUA + "/mf gate remove");
                    sender.sendMessage(ChatColor.AQUA + "/mf gate cancel");
                }
                if (args[1].equalsIgnoreCase("6")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 6/6 == " + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf duel challenge (player) (<optional>time limit in seconds)");
                    sender.sendMessage(ChatColor.AQUA + "/mf duel accept (<optional>player)");
                    sender.sendMessage(ChatColor.AQUA + "/mf duel cancel");
                    sender.sendMessage(ChatColor.AQUA + "/mf bypass - Bypass protections.");
                    sender.sendMessage(ChatColor.AQUA + "/mf config show - Show config values.");
                    sender.sendMessage(ChatColor.AQUA + "/mf config set (option) (value) - Set a config value.");
                    sender.sendMessage(ChatColor.AQUA + "/mf force - Force the plugin to perform certain actions." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf version - Check plugin version." + "\n");
                    sender.sendMessage(ChatColor.AQUA + "/mf resetpowerlevels - Reset player power records and faction cumulative power levels." + "\n");
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
            return false;
        }
        return true;
    }

    static void sendPageOne(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 1/6 == " + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf help # - Show lists of useful commands." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf list - List all factions on the server." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf info - See your faction or another faction's information." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf members - List the members of your faction or another faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf join - Join a faction if you've been invited." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf leave - Leave your current faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf create - Create a new faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf invite - Invite a player to your faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf desc - Set your faction description." + "\n");
    }
}
