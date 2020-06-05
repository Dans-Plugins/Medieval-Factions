package plugin.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand {

    public static boolean sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Medieval Factions Commands" + "\n----------\n");
        sender.sendMessage(ChatColor.AQUA + "/mf help - Show list of useful commands." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf list - List all factions on the server." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf info - See your faction information." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf members - List the members of your faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf join - Join a faction if you've been invited." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf leave - Leave your current faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf create - Create a new faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf invite - Invite a player to your faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf desc - Set your faction description." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf kick - Kick a player from your faction. " + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf transfer - Transfer ownership of your faction to another player.\n");
        sender.sendMessage(ChatColor.AQUA + "/mf delete - Delete your faction (must be owner)." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf declarewar - Declare war against another faction." + "\n");
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.AQUA + "/mf forcesave - Force the plugin to save.");
            sender.sendMessage(ChatColor.AQUA + "/mf forceload - Force the plugin to load.");
        }
        sender.sendMessage(ChatColor.AQUA + "----------\n");
        return true;
    }
}
