package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand {

    int lastPage = 6;

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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage2"), lastPage));
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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage3"), lastPage));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpAutoclaim"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpPromote"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDemote"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpPower"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpSetHome"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpHome"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpWho"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpAlly"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpBreakAlliance"));
                }
                if (args[1].equalsIgnoreCase("4")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage4"), lastPage));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpRename"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpLock"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpUnlock"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGrantAccess"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpCheckAccess"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpRevokeAccess"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpLaws"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpAddLaw"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpRemoveLaw"));
                }
                if (args[1].equalsIgnoreCase("5")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage5"), lastPage));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpEditLaw"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpChat"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpVassalize"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpSwearFealty"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDeclareIndependence"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGrantIndependence"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGateCreate"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGateName"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGateList"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGateRemove"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpGateCancel"));
                }
                if (args[1].equalsIgnoreCase("6")) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage6"), lastPage));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDuelChallenge"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDuelAccept"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDuelCancel"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpPrefix"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpBypass"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpConfigShow"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpConfigSet"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpForce"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpVersion"));
                    sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpResetPowerLevels"));
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.help"));
            return false;
        }
        return true;
    }

    private void sendPageOne(CommandSender sender) {
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CommandsPage1"), lastPage));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpHelp"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpList"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpInfo"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpMembers"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpJoin"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpLeave"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpCreate"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpInvite"));
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("HelpDesc"));
    }
}
