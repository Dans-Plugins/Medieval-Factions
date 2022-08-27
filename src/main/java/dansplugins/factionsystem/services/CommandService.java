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
    private final PlayerService playerService;
    private final MessageService messageService;
    private final Set<SubCommand> subCommands = new HashSet<>();

    public CommandService(LocaleService localeService, MedievalFactions medievalFactions, ConfigService configService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, WarFactory warFactory, Logger logger, Scheduler scheduler, Messenger messenger, RelationChecker relationChecker, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, PlayerService playerService, MessageService messageService) {
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        this.playerService = playerService;
        this.messageService = messageService;
        subCommands.addAll(Arrays.asList(
                new AddLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new AllyCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new AutoClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new BreakAllianceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new BypassCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new ChatCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new CheckAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new CheckClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new ClaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new ConfigCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, playerService, messageService),
                new CreateCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, fiefsIntegrator, currenciesIntegrator, logger, medievalFactions, playerService, messageService),
                new DeclareIndependenceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new DeclareWarCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, warFactory, playerService, messageService),
                new DemoteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new DescCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new DisbandCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, playerService, messageService, medievalFactions),
                new DuelCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, playerService, messageService),
                new EditLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new FlagsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new ForceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, logger, fiefsIntegrator, currenciesIntegrator, playerService, messageService),
                new GateCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, playerService, messageService),
                new GrantAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new GrantIndependenceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new HelpCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new HomeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, scheduler, playerService, messageService),
                new InfoCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, messenger, playerService, messageService),
                new InviteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, playerService, messageService),
                new InvokeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new JoinCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, playerService, messageService),
                new KickCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, playerService, messageService),
                new LawsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new LeaveCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, new DisbandCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, playerService, messageService, medievalFactions), playerService, messageService),
                new ListCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new LockCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, relationChecker, playerService, messageService),
                new MakePeaceCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new MembersCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService, medievalFactions),
                new PowerCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new PrefixCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new PromoteCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new RemoveLawCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new RenameCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, logger, playerService, messageService),
                new ResetPowerLevelsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new RevokeAccessCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new SetHomeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new SwearFealtyCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new TransferCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new UnclaimallCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new UnclaimCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new UnlockCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, relationChecker, playerService, messageService),
                new VassalizeCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, logger, playerService, messageService),
                new VersionCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, medievalFactions, playerService, messageService),
                new WhoCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, messenger, playerService, messageService),
                new MapCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService),
                new StatsCommand(localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService, medievalFactions)
        ));
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") || label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                if (!medievalFactions.USE_NEW_LANGUAGE_FILE) {
                    sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("MedievalFactionsTitle"), medievalFactions.getVersion()));
                    sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("DeveloperList"), medievalFactions.getDescription().getAuthors()));
                    sender.sendMessage(ChatColor.AQUA + localeService.get("WikiLink"));
                    sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("CurrentLanguageID"), configService.getString("languageid")));
                    sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("SupportedLanguageIDList"), localeService.getSupportedLanguageIDsSeparatedByCommas()));
                } else {
                    messageService.getLanguage().getStringList("PluginInfo")
                            .forEach(s -> {
                                s = s.replace("#version#", medievalFactions.getVersion()).replace("#dev#", medievalFactions.getDescription().getAuthors().toString());
                                playerService.sendMessageType(sender, s, s, true);
                            });
                }
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

            playerService.sendMessageType(sender, ChatColor.RED + localeService.get("CommandNotRecognized"), "CommandNotRecognized", false);
        }
        return false;
    }

}
