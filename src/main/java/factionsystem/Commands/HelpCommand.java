package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand {

    public boolean sendHelpMessage(CommandSender sender, String[] args) {

        if (args.length == 1 || args.length == 0) {
            sendPageOne(sender);
        }

        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("1")) {
                sendPageOne(sender);
            }
            if (args[1].equalsIgnoreCase("2")) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 2/5 == " + "\n");
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
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 3/5 == " + "\n");
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
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 4/5 == " + "\n");
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
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 5/5 == " + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf editlaw - Edit an already existing law in your faction.");
                sender.sendMessage(ChatColor.AQUA + "/mf forcesave - Force the plugin to save." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf version - Check plugin version." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf forceload - Force the plugin to load." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf resetpowerlevels - Reset player power records and faction cumulative power levels." + "\n");
            }
        }
        return true;
    }

    static void sendPageOne(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n == Medieval Factions Commands Page 1/5 == " + "\n");
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
