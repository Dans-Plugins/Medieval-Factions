/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.AddLawCommand;
import dansplugins.factionsystem.commands.AllyCommand;
import dansplugins.factionsystem.commands.AutoClaimCommand;
import dansplugins.factionsystem.commands.BreakAllianceCommand;
import dansplugins.factionsystem.commands.BypassCommand;
import dansplugins.factionsystem.commands.ChatCommand;
import dansplugins.factionsystem.commands.CheckAccessCommand;
import dansplugins.factionsystem.commands.CheckClaimCommand;
import dansplugins.factionsystem.commands.ClaimCommand;
import dansplugins.factionsystem.commands.ConfigCommand;
import dansplugins.factionsystem.commands.CreateCommand;
import dansplugins.factionsystem.commands.DeclareIndependenceCommand;
import dansplugins.factionsystem.commands.DeclareWarCommand;
import dansplugins.factionsystem.commands.DemoteCommand;
import dansplugins.factionsystem.commands.DescCommand;
import dansplugins.factionsystem.commands.DisbandCommand;
import dansplugins.factionsystem.commands.DuelCommand;
import dansplugins.factionsystem.commands.EditLawCommand;
import dansplugins.factionsystem.commands.FlagsCommand;
import dansplugins.factionsystem.commands.ForceCommand;
import dansplugins.factionsystem.commands.GateCommand;
import dansplugins.factionsystem.commands.GrantAccessCommand;
import dansplugins.factionsystem.commands.GrantIndependenceCommand;
import dansplugins.factionsystem.commands.HelpCommand;
import dansplugins.factionsystem.commands.HomeCommand;
import dansplugins.factionsystem.commands.InfoCommand;
import dansplugins.factionsystem.commands.InviteCommand;
import dansplugins.factionsystem.commands.InvokeCommand;
import dansplugins.factionsystem.commands.JoinCommand;
import dansplugins.factionsystem.commands.KickCommand;
import dansplugins.factionsystem.commands.LawsCommand;
import dansplugins.factionsystem.commands.LeaveCommand;
import dansplugins.factionsystem.commands.ListCommand;
import dansplugins.factionsystem.commands.LockCommand;
import dansplugins.factionsystem.commands.MakePeaceCommand;
import dansplugins.factionsystem.commands.MapCommand;
import dansplugins.factionsystem.commands.MembersCommand;
import dansplugins.factionsystem.commands.PowerCommand;
import dansplugins.factionsystem.commands.PrefixCommand;
import dansplugins.factionsystem.commands.PromoteCommand;
import dansplugins.factionsystem.commands.RemoveLawCommand;
import dansplugins.factionsystem.commands.RenameCommand;
import dansplugins.factionsystem.commands.ResetPowerLevelsCommand;
import dansplugins.factionsystem.commands.RevokeAccessCommand;
import dansplugins.factionsystem.commands.SetHomeCommand;
import dansplugins.factionsystem.commands.StatsCommand;
import dansplugins.factionsystem.commands.SwearFealtyCommand;
import dansplugins.factionsystem.commands.TransferCommand;
import dansplugins.factionsystem.commands.UnclaimCommand;
import dansplugins.factionsystem.commands.UnclaimallCommand;
import dansplugins.factionsystem.commands.UnlockCommand;
import dansplugins.factionsystem.commands.VassalizeCommand;
import dansplugins.factionsystem.commands.VersionCommand;
import dansplugins.factionsystem.commands.WhoCommand;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.utils.Locale;

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
                new WhoCommand(), new MapCommand(), new StatsCommand()
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
                sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("MedievalFactionsTitle"), MedievalFactions.getInstance().getVersion()));
                sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("DeveloperList"), "DanTheTechMan, Pasarus, Caibinus, Callum, Richardhyy, Mitras2, Kaonami"));
                sender.sendMessage(ChatColor.AQUA + Locale.get("WikiLink"));
                sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("CurrentLanguageID"), MedievalFactions.getInstance().getConfig().getString("languageid")));
                sender.sendMessage(ChatColor.AQUA + String.format(Locale.get("SupportedLanguageIDList"), LocalLocaleService.getInstance().getSupportedLanguageIDsSeparatedByCommas()));
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

            sender.sendMessage(ChatColor.RED + Locale.get("CommandNotRecognized"));
        }
        return false;
    }

}
