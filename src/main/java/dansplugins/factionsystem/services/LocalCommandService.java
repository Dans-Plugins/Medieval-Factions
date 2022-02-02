/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel McCoy Stephenson
 */
public class LocalCommandService {

    private static LocalCommandService instance;
    private final Set<SubCommand> subCommands = new HashSet<>();

    private LocalCommandService() {
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
                new WhoCommand(), new MapCommand(), new StatsCommand(), new LineClaimCommand()
        ));
    }

    public static LocalCommandService getInstance() {
        if (instance == null) instance = new LocalCommandService();
        return instance;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") || label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("MedievalFactionsTitle"), MedievalFactions.getInstance().getVersion()));
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("DeveloperList"), "DanTheTechMan, Pasarus, Caibinus, Callum, Richardhyy, Mitras2, Kaonami"));
                sender.sendMessage(ChatColor.AQUA + LocalLocaleService.getInstance().getText("WikiLink"));
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("CurrentLanguageID"), MedievalFactions.getInstance().getConfig().getString("languageid")));
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("SupportedLanguageIDList"), LocalLocaleService.getInstance().getSupportedLanguageIDsSeparatedByCommas()));
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

            sender.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CommandNotRecognized"));
        }
        return false;
    }

}
