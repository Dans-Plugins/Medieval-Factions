package dansplugins.factionsystem;

import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.managers.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CommandInterpreter {

    private static CommandInterpreter instance;
    private final Set<SubCommand> subCommands = new HashSet<>();

    private CommandInterpreter() {
        subCommands.addAll(Arrays.asList(
                new AddLawCommand(), new AllyCommand(), new AutoClaimCommand(), new BreakAllianceCommand(),
                new BypassCommand(), new ChatCommand(), new CheckAccessCommand(), new CheckClaimCommand(),
                new ClaimCommand(), new ConfigCommand(), new CreateCommand(), new DeclareIndependenceCommand(),
                new DeclareWarCommand(), new DemoteCommand(), new DescCommand(), new DisbandCommand(),
                new DuelCommand(), new EditLawCommand(), new FlagsCommand(), new ForceCommand(), new GateCommand(),
                new GrantAccessCommand(), new GrantIndependenceCommand(), new HelpCommand(), new HomeCommand(),
                new InfoCommand(), new InviteCommand(), new InvokeCommand(), new JoinCommand(), new KickCommand(),
                new LawsCommand(), new LeaveCommand(), new ListCommand(), new LockCommand(), new MakePeaceCommand(),
                new MembersCommand(), new PowerCommand(), new PrefixCommand(), new PromoteCommand(),
                new RemoveLawCommand(), new RenameCommand(), new ResetPowerLevelsCommand(), new RevokeAccessCommand(),
                new SetHomeCommand(), new SwearFealtyCommand(), new TransferCommand(), new UnclaimallCommand(),
                new UnclaimCommand(), new UnlockCommand(), new VassalizeCommand(), new VersionCommand(),
                new WhoCommand()
        ));
    }

    public static CommandInterpreter getInstance() {
        if (instance == null) instance = new CommandInterpreter();
        return instance;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") || label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("MedievalFactionsTitle"), MedievalFactions.getInstance().getVersion()));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("DeveloperList"), "DanTheTechMan, Pasarus, Caibinus, Callum"));
                sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("WikiLink"));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CurrentLanguageID"), MedievalFactions.getInstance().getConfig().getString("languageid")));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("SupportedLanguageIDList"), LocaleManager.getInstance().getSupportedLanguageIDsSeparatedByCommas()));
                return true;
            }

            // Loop through SubCommands.
            for (SubCommand subCommand : subCommands) {
                if (subCommand.isCommand(args[0])) { // If it matches, execute.
                    String[] arguments = new String[args.length - 1]; // Take first argument out of Array.
                    System.arraycopy(args, 1, arguments, 0, arguments.length);
                    subCommand.performCommand(sender, arguments, args[0]); // Execute!
                    return true; // Return true as the command was found and run.
                }
            }

            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CommandNotRecognized"));
        }
        return false;
    }

}
