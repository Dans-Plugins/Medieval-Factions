package factionsystem.Subsystems;

import factionsystem.Commands.*;
import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class CommandSubsystem {

    Main main = null;

    public CommandSubsystem(Main plugin) {
        main = plugin;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") ||
                label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + " == Medieval Factions " + main.version + " == ");
                sender.sendMessage(ChatColor.AQUA + "Developers: DanTheTechMan, Pasarus, Caibinus");
                sender.sendMessage(ChatColor.AQUA + "Wiki: https://github.com/DansPlugins/Medieval-Factions/wiki");
                return true;
            }

            // argument check
            if (args.length > 0) {

                // default commands ----------------------------------------------------------------------------------

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
                        HelpCommand command = new HelpCommand();
                        command.sendHelpMessage(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
                        return false;
                    }
                }

                // create command
                if (args[0].equalsIgnoreCase("create") ) {
                    if (sender.hasPermission("mf.create")|| sender.hasPermission("mf.default")) {
                        CreateCommand command = new CreateCommand(main);
                        command.createFaction(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.create'");
                        return false;
                    }
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    if (sender.hasPermission("mf.list") || sender.hasPermission("mf.default")) {
                        ListCommand command = new ListCommand(main);
                        command.listFactions(sender);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.list'");
                        return false;
                    }
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband")) {
                    if (sender.hasPermission("mf.disband") || sender.hasPermission("mf.default")) {
                        DisbandCommand command = new DisbandCommand(main);
                        command.deleteFaction(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.disband'");
                        return false;
                    }
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    if (sender.hasPermission("mf.members") || sender.hasPermission("mf.default")) {
                        MembersCommand command = new MembersCommand(main);
                        command.showMembers(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.members'");
                        return false;
                    }
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    if (sender.hasPermission("mf.info") || sender.hasPermission("mf.default")) {
                        InfoCommand command = new InfoCommand(main);
                        command.showInfo(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.info'");
                        return false;
                    }

                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    if (sender.hasPermission("mf.desc") || sender.hasPermission("mf.default")) {
                        DescCommand command = new DescCommand(main);
                        command.setDescription(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.desc'");
                        return false;
                    }

                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    if (sender.hasPermission("mf.invite") || sender.hasPermission("mf.default")) {
                        InviteCommand command = new InviteCommand(main);
                        command.invitePlayer(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.invite'");
                        return false;
                    }
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    if (sender.hasPermission("mf.join") || sender.hasPermission("mf.default")) {
                        JoinCommand command = new JoinCommand(main);
                        command.joinFaction(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.join'");
                        return false;
                    }
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    if (sender.hasPermission("mf.kick") || sender.hasPermission("mf.default")) {
                        KickCommand command = new KickCommand(main);
                        command.kickPlayer(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.kick'");
                        return false;
                    }
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    if (sender.hasPermission("mf.leave") || sender.hasPermission("mf.default")) {
                        LeaveCommand command = new LeaveCommand(main);
                        command.leaveFaction(sender);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.leave'");
                        return false;
                    }
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    if (sender.hasPermission("mf.transfer") || sender.hasPermission("mf.default")) {
                        TransferCommand command = new TransferCommand(main);
                        command.transferOwnership(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.transfer'");
                        return false;
                    }
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar") || args[0].equalsIgnoreCase("dw")) {
                    if (sender.hasPermission("mf.declarewar") || sender.hasPermission("mf.default")) {
                        DeclareWarCommand command = new DeclareWarCommand(main);
                        command.declareWar(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.declarewar'");
                        return false;
                    }

                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace") || args[0].equalsIgnoreCase("mp")) {
                    if (sender.hasPermission("mf.makepeace") || sender.hasPermission("mf.default")) {
                        MakePeaceCommand command = new MakePeaceCommand(main);
                        command.makePeace(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.makepeace'");
                        return false;
                    }
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    ClaimCommand command = new ClaimCommand(main);
                    return command.claimChunk(sender, args);
                }

                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim")) {
                    if (sender.hasPermission("mf.unclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (isInFaction(player.getUniqueId(), main.factions)) {
                                main.utilities.removeChunkAtPlayerLocation(player);
                                return true;
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                                return false;
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaim'");
                        return false;
                    }
                }

                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall") || args[0].equalsIgnoreCase("ua")) {
                    if (sender instanceof Player) {

                        Player player = (Player) sender;

                        if (args.length > 1) {
                            if (player.hasPermission("mf.unclaimall.others") || player.hasPermission("mf.admin")) {

                                String factionName = createStringFromFirstArgOnwards(args);

                                Faction faction = getFaction(factionName, main.factions);

                                if (faction != null) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    removeAllClaimedChunks(faction.getName(), main.claimedChunks);
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed from " + factionName + "!");

                                    // remove locks associated with this faction
                                    removeAllLocks(faction.getName(), main.lockedBlocks);
                                    return true;
                                } else {
                                    player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                                    return false;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.unclaimall.others'");
                                return false;
                            }
                        }

                        if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {

                            for (Faction faction : main.factions) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    removeAllClaimedChunks(faction.getName(), main.claimedChunks);
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed.");

                                    // remove locks associated with this faction
                                    removeAllLocks(faction.getName(), main.lockedBlocks);
                                    return true;
                                }
                            }
                            player.sendMessage(ChatColor.RED + "You're not in a faction!");
                            return false;
                        }
                        else {
                            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
                            return false;
                        }
                    }
                }

                // checkclaim command
                if (args[0].equalsIgnoreCase("checkclaim")|| args[0].equalsIgnoreCase("cc")) {
                    if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            String result = main.utilities.checkOwnershipAtPlayerLocation(player);
                            if (result.equalsIgnoreCase("unclaimed")) {
                                player.sendMessage(ChatColor.GREEN + "This land is unclaimed.");
                                return true;
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "This land is claimed by " + result + ".");
                                return false;
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
                        return false;
                    }
                }

                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim")|| args[0].equalsIgnoreCase("ac")) {
                    if (sender.hasPermission("mf.autoclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            if (isInFaction(player.getUniqueId(), main.factions)) {
                                boolean owner = false;
                                for (Faction faction : main.factions) {
                                    if (faction.isOwner(player.getUniqueId())) {
                                        owner = true;
                                        faction.toggleAutoClaim();
                                        player.sendMessage(ChatColor.AQUA + "Autoclaim toggled.");
                                        return true;
                                    }

                                }
                                if (!owner) {
                                    player.sendMessage(ChatColor.RED + "You must be the owner to use this command.");
                                    return false;
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                                return false;
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.autoclaim'");
                        return false;
                    }
                }

                // promote command
                if (args[0].equalsIgnoreCase("promote")) {
                    if (sender.hasPermission("mf.promote") || sender.hasPermission("mf.default")) {
                        PromoteCommand command = new PromoteCommand(main);
                        command.promotePlayer(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.promote'");
                        return false;
                    }
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    if (sender.hasPermission("mf.demote") || sender.hasPermission("mf.default")) {
                        DemoteCommand command = new DemoteCommand(main);
                        command.demotePlayer(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.demote'");
                        return false;
                    }
                }


                // power command
                if  (args[0].equalsIgnoreCase("power")) {
                    PowerCommand command = new PowerCommand(main);
                    command.powerCheck(sender, args);
                    return true;
                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome")|| args[0].equalsIgnoreCase("sh")) {
                    if (sender.hasPermission("mf.sethome") || sender.hasPermission("mf.default")) {
                        SetHomeCommand command = new SetHomeCommand(main);
                        command.setHome(sender);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.sethome'");
                        return false;
                    }
                }

                // home command
                if (args[0].equalsIgnoreCase("home")) {
                    if (sender.hasPermission("mf.home") || sender.hasPermission("mf.default")) {
                        HomeCommand command = new HomeCommand(main);
                        command.teleportPlayer(sender);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.home'");
                        return false;
                    }
                }

                // version command
                if (args[0].equalsIgnoreCase("version")) {
                    if (sender.hasPermission("mf.version") || sender.hasPermission("mf.default")) {
                        sender.sendMessage(ChatColor.AQUA + "Medieval-Factions-" + main.version);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.version'");
                        return false;
                    }

                }

                // who command
                if (args[0].equalsIgnoreCase("who")) {
                    if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                        WhoCommand command = new WhoCommand(main);
                        command.sendInformation(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.who'");
                        return false;
                    }

                }

                // ally command
                if (args[0].equalsIgnoreCase("ally")) {
                    if (sender.hasPermission("mf.ally") || sender.hasPermission("mf.default")) {
                        AllyCommand command = new AllyCommand(main);
                        command.requestAlliance(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.ally'");
                        return false;
                    }

                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance")|| args[0].equalsIgnoreCase("ba")) {
                    if (sender.hasPermission("mf.breakalliance") || sender.hasPermission("mf.default")) {
                        BreakAllianceCommand command = new BreakAllianceCommand(main);
                        command.breakAlliance(sender, args);
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.breakalliance'");
                        return false;
                    }
                }

                // TODO: shift responsibility of perm checking from Main to the Command class of each command, like below

                // rename command
                if (args[0].equalsIgnoreCase("rename")) {
                    RenameCommand command = new RenameCommand(main);
                    command.renameFaction(sender, args);
                    return true;
                }

                // lock command
                if (args[0].equalsIgnoreCase("lock")) {
                    LockCommand command = new LockCommand(main);
                    command.lockBlock(sender, args);
                    return true;
                }

                // unlock command
                if (args[0].equalsIgnoreCase("unlock")) {
                    UnlockCommand command = new UnlockCommand(main);
                    command.unlockBlock(sender, args);
                    return true;
                }

                // grantaccess command
                if (args[0].equalsIgnoreCase("grantaccess")|| args[0].equalsIgnoreCase("ga")) {
                    GrantAccessCommand command = new GrantAccessCommand(main);
                    command.grantAccess(sender, args);
                    return true;
                }

                // checkaccess command
                if (args[0].equalsIgnoreCase("checkaccess")|| args[0].equalsIgnoreCase("ca")) {
                    CheckAccessCommand command = new CheckAccessCommand(main);
                    command.checkAccess(sender, args);
                    return true;
                }

                // revokeaccess command
                if (args[0].equalsIgnoreCase("revokeaccess")|| args[0].equalsIgnoreCase("ra")) {
                    RevokeAccessCommand command = new RevokeAccessCommand(main);
                    command.revokeAccess(sender, args);
                    return true;
                }

                // laws command
                if (args[0].equalsIgnoreCase("laws")) {
                    LawsCommand command = new LawsCommand(main);
                    command.showLawsToPlayer(sender, args);
                    return true;
                }

                // addlaw command
                if (args[0].equalsIgnoreCase("addlaw")|| args[0].equalsIgnoreCase("al")) {
                    AddLawCommand command = new AddLawCommand(main);
                    command.addLaw(sender, args);
                    return true;
                }

                // removelaw command
                if (args[0].equalsIgnoreCase("removelaw")|| args[0].equalsIgnoreCase("rl")) {
                    RemoveLawCommand command = new RemoveLawCommand(main);
                    command.removeLaw(sender, args);
                    return true;
                }

                // editlaw command
                if (args[0].equalsIgnoreCase("editlaw") || args[0].equalsIgnoreCase("el")) {
                    EditLawCommand command = new EditLawCommand(main);
                    command.editLaw(sender, args);
                    return true;
                }

                // chat command
                if (args[0].equalsIgnoreCase("chat")) {
                    ChatCommand command = new ChatCommand(main);
                    command.toggleFactionChat(sender);
                    return true;
                }

                // vassalize command
                if (args[0].equalsIgnoreCase("vassalize")) {
                    VassalizeCommand command = new VassalizeCommand(main);
                    command.sendVassalizationOffer(sender, args);
                    return true;
                }

                // swearfealty command
                if (args[0].equalsIgnoreCase("swearfealty") || args[0].equalsIgnoreCase("sf")) {
                    SwearFealtyCommand command = new SwearFealtyCommand(main);
                    command.swearFealty(sender, args);
                    return true;
                }

                // declare independence command
                if (args[0].equalsIgnoreCase("declareindependence") || args[0].equalsIgnoreCase("di")) {
                    DeclareIndependenceCommand command = new DeclareIndependenceCommand(main);
                    command.declareIndependence(sender);
                    return true;
                }

                // grant independence command
                if (args[0].equalsIgnoreCase("grantindependence") || args[0].equalsIgnoreCase("gi")) {
                    GrantIndependenceCommand command = new GrantIndependenceCommand(main);
                    command.grantIndependence(sender, args);
                    return true;
                }

                // gate management commands
                if (args[0].equalsIgnoreCase("gate") || args[0].equalsIgnoreCase("gt")) {
                	GateCommand command = new GateCommand(main);
                	command.handleGate(sender, args);
                	return true;
                }
                
                // admin commands ----------------------------------------------------------------------------------

                // force command
                if (args[0].equalsIgnoreCase("force")) {
                    ForceCommand command = new ForceCommand(main);
                    return command.force(sender, args);
                }

                // reset power levels command
                if (args[0].equalsIgnoreCase("resetpowerlevels")|| args[0].equalsIgnoreCase("rpl")) {
                    if (sender.hasPermission("mf.resetpowerlevels") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Power level resetting...");
                        main.utilities.resetPowerRecords();
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.resetpowerlevels'");
                        return false;
                    }
                }

                // bypass command
                if (args[0].equalsIgnoreCase("bypass")) {
                    BypassCommand command = new BypassCommand(main);
                    command.toggleBypass(sender);
                    return true;
                }

                // config command
                if (args[0].equalsIgnoreCase("config")) {
                    ConfigCommand command = new ConfigCommand(main);
                    command.handleConfigAccess(sender, args);
                    return true;
                }

            }
            sender.sendMessage(ChatColor.RED + "Medieval Factions doesn't recognize that command!");
        }
        return false;
    }

}
