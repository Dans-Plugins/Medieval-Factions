package dansplugins.factionsystem;

import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.commands.abs.SubCommand;
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
                new DuelCommand(), new EditLawCommand(), new ForceCommand(), new GateCommand(),
                new GrantAccessCommand(), new GrantIndependenceCommand(), new HelpCommand(), new HomeCommand(),
                new InfoCommand(), new InviteCommand(), new InvokeCommand(), new JoinCommand(), new KickCommand(),
                new LawsCommand(), new LeaveCommand(), new ListCommand(), new LockCommand(), new MakePeaceCommand(),
                new MembersCommand(), new PowerCommand(), new PrefixCommand(), new PromoteCommand(),
                new RemoveLawCommand()
        ));
    }

    public static CommandInterpreter getInstance() {
        if (instance == null) instance = new CommandInterpreter();
        return instance;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") ||
                label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("MedievalFactionsTitle"), MedievalFactions.getInstance().getVersion()));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("DeveloperList"), "DanTheTechMan, Pasarus, Caibinus"));
                sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("WikiLink"));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("CurrentLanguageID"), MedievalFactions.getInstance().getConfig().getString("languageid")));
                sender.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("SupportedLanguageIDList"), LocaleManager.getInstance().getSupportedLanguageIDsSeparatedByCommas()));
                return true;
            }

            // argument check
            else {

                // default commands ----------------------------------------------------------------------------------

                // help command
                if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdHelp"))) {
                    HelpCommand command = new HelpCommand();
                    command.sendHelpMessage(sender, args);
                    return true;
                }

                // create command
                if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdCreate"))) {
                    CreateCommand command = new CreateCommand();
                    command.createFaction(sender, args);
                    return true;
                }

                // list command
                if  (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdList"))) {
                    ListCommand command = new ListCommand();
                    command.listFactions(sender);
                    return true;
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDisband"))) {
                    DisbandCommand command = new DisbandCommand();
                    command.deleteFaction(sender, args);
                    return true;
                }

                // members command
                if (args[0].equalsIgnoreCase("members") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdMembers"))) {
                    MembersCommand command = new MembersCommand();
                    command.showMembers(sender, args);
                    return true;
                }

                // info command
                if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdInfo"))) {
                    InfoCommand command = new InfoCommand();
                    command.showInfo(sender, args);
                    return true;
                }

                // desc command
                if (args[0].equalsIgnoreCase("desc") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDesc"))) {
                    DescCommand command = new DescCommand();
                    command.setDescription(sender, args);
                    return true;
                }

                // invite command
                if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdInvite"))) {
                    InviteCommand command = new InviteCommand();
                    command.invitePlayer(sender, args);
                    return true;
                }

                // join command
                if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdJoin"))) {
                    JoinCommand command = new JoinCommand();
                    command.joinFaction(sender, args);
                    return true;
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdKick"))) {
                    KickCommand command = new KickCommand();
                    command.kickPlayer(sender, args);
                    return true;
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdLeave"))) {
                    LeaveCommand command = new LeaveCommand();
                    command.leaveFaction(sender);
                    return true;
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdTransfer"))) {
                    TransferCommand command = new TransferCommand();
                    command.transferOwnership(sender, args);
                    return true;
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDeclareWar")) || args[0].equalsIgnoreCase("dw")) {
                    DeclareWarCommand command = new DeclareWarCommand();
                    command.declareWar(sender, args);
                    return true;
                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdMakePeace")) || args[0].equalsIgnoreCase("mp")) {
                    MakePeaceCommand command = new MakePeaceCommand();
                    command.makePeace(sender, args);
                    return true;
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdClaim"))) {
                    ClaimCommand command = new ClaimCommand();
                    return command.claim(sender, args);
                }

                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdUnclaim"))) {
                    UnclaimCommand command = new UnclaimCommand();
                    return command.unclaim(sender);
                }

                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdUnclaimall")) || args[0].equalsIgnoreCase("ua")) {
                    UnclaimallCommand command = new UnclaimallCommand();
                    return command.unclaimAllLand(sender, args);
                }

                // checkclaim command
                if (args[0].equalsIgnoreCase("checkclaim") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdCheckClaim"))|| args[0].equalsIgnoreCase("cc")) {
                    CheckClaimCommand command = new CheckClaimCommand();
                    return command.showClaim(sender);
                }

                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdAutoClaim"))|| args[0].equalsIgnoreCase("ac")) {
                    AutoClaimCommand command = new AutoClaimCommand();
                    return command.toggleAutoClaim(sender);
                }

                // promote command
                if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdPromote"))) {
                    PromoteCommand command = new PromoteCommand();
                    command.promotePlayer(sender, args);
                    return true;
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDemote"))) {
                    DemoteCommand command = new DemoteCommand();
                    command.demotePlayer(sender, args);
                    return true;
                }

                // power command
                if  (args[0].equalsIgnoreCase("power") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdPower"))) {
                    PowerCommand command = new PowerCommand();
                    command.powerCheck(sender, args);
                    return true;
                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdSetHome"))|| args[0].equalsIgnoreCase("sh")) {
                    SetHomeCommand command = new SetHomeCommand();
                    command.setHome(sender);
                    return true;
                }

                // home command
                if (args[0].equalsIgnoreCase("home") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdHome"))) {
                    HomeCommand command = new HomeCommand();
                    command.teleportPlayer(sender);
                    return true;
                }

                // version command
                if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdVersion"))) {
                    VersionCommand command = new VersionCommand();
                    return command.showVersion(sender);
                }

                // who command
                if (args[0].equalsIgnoreCase("who") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdWho"))) {
                    WhoCommand command = new WhoCommand();
                    command.sendInformation(sender, args);
                    return true;
                }

                // ally command
                if (args[0].equalsIgnoreCase("ally") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdAlly"))) {
                    AllyCommand command = new AllyCommand();
                    command.requestAlliance(sender, args);
                    return true;
                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdBreakAlliance"))|| args[0].equalsIgnoreCase("ba")) {
                    BreakAllianceCommand command = new BreakAllianceCommand();
                    command.breakAlliance(sender, args);
                    return true;
                }

                // rename command
                if (args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdRename"))) {
                    RenameCommand command = new RenameCommand();
                    command.renameFaction(sender, args);
                    return true;
                }

                // lock command
                if (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdLock"))) {
                    LockCommand command = new LockCommand();
                    command.lockBlock(sender, args);
                    return true;
                }

                // unlock command
                if (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdUnlock"))) {
                    UnlockCommand command = new UnlockCommand();
                    command.unlockBlock(sender, args);
                    return true;
                }

                // grantaccess command
                if (args[0].equalsIgnoreCase("grantaccess") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGrantAccess")) || args[0].equalsIgnoreCase("ga")) {
                    GrantAccessCommand command = new GrantAccessCommand();
                    command.grantAccess(sender, args);
                    return true;
                }

                // checkaccess command
                if (args[0].equalsIgnoreCase("checkaccess") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdCheckAccess"))|| args[0].equalsIgnoreCase("ca")) {
                    CheckAccessCommand command = new CheckAccessCommand();
                    command.checkAccess(sender, args);
                    return true;
                }

                // revokeaccess command
                if (args[0].equalsIgnoreCase("revokeaccess") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdRevokeAccess"))|| args[0].equalsIgnoreCase("ra")) {
                    RevokeAccessCommand command = new RevokeAccessCommand();
                    command.revokeAccess(sender, args);
                    return true;
                }

                // laws command
                if (args[0].equalsIgnoreCase("laws") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdLaws"))) {
                    LawsCommand command = new LawsCommand();
                    command.showLawsToPlayer(sender, args);
                    return true;
                }

                // addlaw command
                if (args[0].equalsIgnoreCase("addlaw") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdAddLaw"))|| args[0].equalsIgnoreCase("al")) {
                    AddLawCommand command = new AddLawCommand();
                    command.addLaw(sender, args);
                    return true;
                }

                // removelaw command
                if (args[0].equalsIgnoreCase("removelaw") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdRemoveLaw"))|| args[0].equalsIgnoreCase("rl")) {
                    RemoveLawCommand command = new RemoveLawCommand();
                    command.removeLaw(sender, args);
                    return true;
                }

                // editlaw command
                if (args[0].equalsIgnoreCase("editlaw") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdEditLaw")) || args[0].equalsIgnoreCase("el")) {
                    EditLawCommand command = new EditLawCommand();
                    command.editLaw(sender, args);
                    return true;
                }

                // chat command
                if (args[0].equalsIgnoreCase("chat") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdChat"))) {
                    ChatCommand command = new ChatCommand();
                    command.toggleFactionChat(sender);
                    return true;
                }

                // vassalize command
                if (args[0].equalsIgnoreCase("vassalize") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdVassalize"))) {
                    VassalizeCommand command = new VassalizeCommand();
                    command.sendVassalizationOffer(sender, args);
                    return true;
                }

                // swearfealty command
                if (args[0].equalsIgnoreCase("swearfealty") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdSwearFealty")) || args[0].equalsIgnoreCase("sf")) {
                    SwearFealtyCommand command = new SwearFealtyCommand();
                    command.swearFealty(sender, args);
                    return true;
                }

                // declare independence command
                if (args[0].equalsIgnoreCase("declareindependence") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDeclareIndependence")) || args[0].equalsIgnoreCase("di")) {
                    DeclareIndependenceCommand command = new DeclareIndependenceCommand();
                    command.declareIndependence(sender);
                    return true;
                }

                // grant independence command
                if (args[0].equalsIgnoreCase("grantindependence") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGrantIndependence")) || args[0].equalsIgnoreCase("gi")) {
                    GrantIndependenceCommand command = new GrantIndependenceCommand();
                    command.grantIndependence(sender, args);
                    return true;
                }

                // gate management commands
                if (args[0].equalsIgnoreCase("gate") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGate")) || args[0].equalsIgnoreCase("gt")) {
                	GateCommand command = new GateCommand();
                	command.handleGate(sender, args);
                	return true;
                }

                // duel command
                if (args[0].equalsIgnoreCase("duel") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDuel")) || args[0].equalsIgnoreCase("dl")) {
                	DuelCommand command = new DuelCommand();
                	command.handleDuel(sender, args);
                	return true;
                }

                // invoke command
                if (args[0].equalsIgnoreCase("invoke") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdInvoke"))) {
                    InvokeCommand command = new InvokeCommand();
                    return command.invokeAlliance(sender, args);
                }

                // prefix command
                if (args[0].equalsIgnoreCase("prefix") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdPrefix"))) {
                    PrefixCommand command = new PrefixCommand();
                    return command.changePrefix(sender, args);
                }

                // admin commands ----------------------------------------------------------------------------------

                // force command
                if (args[0].equalsIgnoreCase("force") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForce"))) {
                    ForceCommand command = new ForceCommand();
                    return command.force(sender, args);
                }

                // reset power levels command
                if (args[0].equalsIgnoreCase("resetpowerlevels") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdResetPowerLevels"))|| args[0].equalsIgnoreCase("rpl")) {
                    ResetPowerLevelsCommand command = new ResetPowerLevelsCommand();
                    return command.resetPowerLevels(sender);
                }

                // bypass command
                if (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdBypass"))) {
                    BypassCommand command = new BypassCommand();
                    command.toggleBypass(sender);
                    return true;
                }

                // config command
                if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdConfig"))) {
                    ConfigCommand command = new ConfigCommand();
                    command.handleConfigAccess(sender, args);
                    return true;
                }

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
