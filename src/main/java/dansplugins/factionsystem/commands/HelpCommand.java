package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    // Each page of the help command should have a title and nine commands. This is for ease of use.
    public boolean sendHelpMessage(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.help")) {
            if (args.length == 1 || args.length == 0) {
                sendPageOne(sender);
            }

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("1")) {
                    sendPageOne(sender);
                }
                if (args[1].equalsIgnoreCase("2")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage2"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpKick"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpTransfer"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDisband"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDeclareWar"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpMakePeace"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpInvoke"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpClaim"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpUnclaim"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpUnclaimall"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpCheckClaim"));
                }
                if (args[1].equalsIgnoreCase("3")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage3"));
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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage4"));
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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage5"));
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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage6"));
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
            sender.sendMessage(ChatColor.RED +LocaleManager.getInstance().getText("PermissionHelp"));
            return false;
        }
        return true;
    }

    static void sendPageOne(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + LocaleManager.getInstance().getText("CommandsPage1"));
        sender.sendMessage(ChatColor.AQUA + "/mf help # - Show lists of useful commands." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf list - List all factions on the server." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf info - See your faction or another faction's information." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf members - List the members of your faction or another faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf join (faction) - Join a faction if you've been invited." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf leave - Leave your current faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf create (name) - Create a new faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf invite (player) - Invite a player to your faction." + "\n");
        sender.sendMessage(ChatColor.AQUA + "/mf desc (desc) - Set your faction description." + "\n");
    }
}
