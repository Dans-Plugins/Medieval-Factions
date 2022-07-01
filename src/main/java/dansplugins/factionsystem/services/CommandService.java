/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.RelationChecker;
import dansplugins.factionsystem.utils.extended.Messenger;
import dansplugins.factionsystem.utils.extended.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel McCoy Stephenson
 */
public class CommandService {
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final ConfigService configService;

    private final Set<SubCommand> subCommands = new HashSet<>();

    public CommandService(LocaleService localeService, MedievalFactions medievalFactions, ConfigService configService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, WarFactory warFactory, Logger logger, Scheduler scheduler, Messenger messenger, RelationChecker relationChecker, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator) {
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        subCommands.addAll(Arrays.asList(
                new AddLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new AllyCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new AutoClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new BreakAllianceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new BypassCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new ChatCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new CheckAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new CheckClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new ClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new ConfigCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions),
                new CreateCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, fiefsIntegrator, currenciesIntegrator, logger, medievalFactions),
                new DeclareIndependenceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new DeclareWarCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, warFactory),
                new DemoteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new DescCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new DisbandCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger),
                new DuelCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions),
                new EditLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new FlagsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new ForceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, logger, fiefsIntegrator, currenciesIntegrator),
                new GateCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions),
                new GrantAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new GrantIndependenceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new HelpCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new HomeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, scheduler),
                new InfoCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, messenger),
                new InviteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions),
                new InvokeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new JoinCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger),
                new KickCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger),
                new LawsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new LeaveCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, new DisbandCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger)),
                new ListCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new LockCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, relationChecker),
                new MakePeaceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new MembersCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new PowerCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new PrefixCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new PromoteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new RemoveLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new RenameCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, logger),
                new ResetPowerLevelsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new RevokeAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new SetHomeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new SwearFealtyCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new TransferCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new UnclaimallCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new UnclaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new UnlockCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, relationChecker),
                new VassalizeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger),
                new VersionCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions),
                new WhoCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, messenger),
                new MapCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService),
                new StatsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService)
        ));
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") || label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("MedievalFactionsTitle"), medievalFactions.getVersion()));
                sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("DeveloperList"), "DanTheTechMan, Pasarus, Caibinus, Callum, Richardhyy, Mitras2, Kaonami"));
                sender.sendMessage(ChatColor.AQUA + localeService.get("WikiLink"));
                sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("CurrentLanguageID"), configService.getString("languageid")));
                sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("SupportedLanguageIDList"), localeService.getSupportedLanguageIDsSeparatedByCommas()));
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

            sender.sendMessage(ChatColor.RED + localeService.get("CommandNotRecognized"));
        }
        return false;
    }

}
