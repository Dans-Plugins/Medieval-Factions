package factionsystem.Subsystems;

import factionsystem.Commands.*;
import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class CommandSubsystem {

    MedievalFactions main = null;

    public CommandSubsystem(MedievalFactions plugin) {
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
                    HelpCommand command = new HelpCommand(main);
                    command.sendHelpMessage(sender, args);
                    return true;
                }

                // create command
                if (args[0].equalsIgnoreCase("create") ) {
                    CreateCommand command = new CreateCommand(main);
                    command.createFaction(sender, args);
                    return true;
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    ListCommand command = new ListCommand(main);
                    command.listFactions(sender);
                    return true;
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband")) {
                    DisbandCommand command = new DisbandCommand(main);
                    command.deleteFaction(sender, args);
                    return true;
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    MembersCommand command = new MembersCommand(main);
                    command.showMembers(sender, args);
                    return true;
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    InfoCommand command = new InfoCommand(main);
                    command.showInfo(sender, args);
                    return true;
                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    DescCommand command = new DescCommand(main);
                    command.setDescription(sender, args);
                    return true;
                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    InviteCommand command = new InviteCommand(main);
                    command.invitePlayer(sender, args);
                    return true;
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    JoinCommand command = new JoinCommand(main);
                    command.joinFaction(sender, args);
                    return true;
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    KickCommand command = new KickCommand(main);
                    command.kickPlayer(sender, args);
                    return true;
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    LeaveCommand command = new LeaveCommand(main);
                    command.leaveFaction(sender);
                    return true;
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    TransferCommand command = new TransferCommand(main);
                    command.transferOwnership(sender, args);
                    return true;
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar") || args[0].equalsIgnoreCase("dw")) {
                    DeclareWarCommand command = new DeclareWarCommand(main);
                    command.declareWar(sender, args);
                    return true;
                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace") || args[0].equalsIgnoreCase("mp")) {
                    MakePeaceCommand command = new MakePeaceCommand(main);
                    command.makePeace(sender, args);
                    return true;
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    if (sender.hasPermission("mf.claim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            // if not at demesne limit
                            if (isInFaction(player.getUniqueId(), main.factions)) {
                                Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);
                                if (getChunksClaimedByFaction(playersFaction.getName(), main.claimedChunks) < playersFaction.getCumulativePowerLevel()) {
                                    main.utilities.addChunkAtPlayerLocation(player);
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                                    return false;
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You must be in a faction to use this command.");
                                return false;
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.claim'");
                        return false;
                    }
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
                    PromoteCommand command = new PromoteCommand(main);
                    command.promotePlayer(sender, args);
                    return true;
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    DemoteCommand command = new DemoteCommand(main);
                    command.demotePlayer(sender, args);
                    return true;
                }

                // power command
                if  (args[0].equalsIgnoreCase("power")) {
                    PowerCommand command = new PowerCommand(main);
                    command.powerCheck(sender, args);
                    return true;
                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome")|| args[0].equalsIgnoreCase("sh")) {
                    SetHomeCommand command = new SetHomeCommand(main);
                    command.setHome(sender);
                    return true;
                }

                // home command
                if (args[0].equalsIgnoreCase("home")) {
                    HomeCommand command = new HomeCommand(main);
                    command.teleportPlayer(sender);
                    return true;
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
                    WhoCommand command = new WhoCommand(main);
                    command.sendInformation(sender, args);
                    return true;
                }

                // ally command
                if (args[0].equalsIgnoreCase("ally")) {
                    AllyCommand command = new AllyCommand(main);
                    command.requestAlliance(sender, args);
                    return true;
                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance")|| args[0].equalsIgnoreCase("ba")) {
                    BreakAllianceCommand command = new BreakAllianceCommand(main);
                    command.breakAlliance(sender, args);
                    return true;
                }

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
                
                if (args[0].equalsIgnoreCase("duel") || args[0].equalsIgnoreCase("dl")) {
                	DuelCommand command = new DuelCommand(main);
                	command.handleDuel(sender, args);
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
