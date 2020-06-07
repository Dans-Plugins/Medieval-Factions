package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand {

    public static boolean sendHelpMessage(CommandSender sender, String[] args) {

        if (args.length == 1 || args.length == 0) {
            sendPageOne(sender);
        }

        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("1")) {
                sendPageOne(sender);
            }
            if (args[1].equalsIgnoreCase("2")) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n----------\n" + "Medieval Factions Commands - Page 2/4" + "\n----------\n");
                sender.sendMessage(ChatColor.AQUA + "/mf create - Create a new faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf invite - Invite a player to your faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf desc - Set your faction description." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf kick - Kick a player from your faction. " + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf transfer - Transfer ownership of your faction to another player.\n");
                sender.sendMessage(ChatColor.AQUA + "/mf delete - Delete your faction (must be owner)." + "\n");
            }
            if (args[1].equalsIgnoreCase("3")) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n----------\n" + "Medieval Factions Commands - Page 3/4" + "\n----------\n");
                sender.sendMessage(ChatColor.AQUA + "/mf declarewar - Declare war against another faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf makepeace - Send peace offer to another faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf claim - Claim land for your faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf unclaim - Unclaim land for your faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf unclaimall - Unclaim all land for your faction." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf checkclaim - Check if land is claimed." + "\n");
            }
            if (args[1].equalsIgnoreCase("4")) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n----------\n" + "Medieval Factions Commands - Page 4/4" + "\n----------\n");
                sender.sendMessage(ChatColor.AQUA + "/mf autoclaim - Toggle auto claim, making land claiming easier." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf promote - Promote a player to officer status." + "\n");
                sender.sendMessage(ChatColor.AQUA + "/mf demote - Demote an officer to member status." + "\n");
            }
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.AQUA + "/mf forcesave - Force the plugin to save.");
            sender.sendMessage(ChatColor.AQUA + "/mf forceload - Force the plugin to load.");
        }
        sender.sendMessage(ChatColor.AQUA + "----------\n");
        return true;
    }

    static void sendPageOne(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "\n----------\n" + "Medieval Factions Commands - Page 1/4" + "\n----------\n");
        sender.sendMessage(ChatColor.AQUA + "/mf help # - Show lists of useful commands." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf list - List all factions on the server." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf info - See your faction or another faction's information." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf members - List the members of your faction or another faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf join - Join a faction if you've been invited." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf leave - Leave your current faction." + "\n");
    }
}
