package dansplugins.factionsystem;

import dansplugins.factionsystem.commands.*;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandInterpreter {

    private static CommandInterpreter instance;

    private CommandInterpreter() {

    }

    public static CommandInterpreter getInstance() {
        if (instance == null) {
            instance = new CommandInterpreter();
        }
        return instance;
    }

    public boolean interpretCommand(CommandSender sender, String label, String[] args) {
        // mf commands
        if (label.equalsIgnoreCase("mf") || label.equalsIgnoreCase("f") ||
                label.equalsIgnoreCase("medievalfactions") || label.equalsIgnoreCase("factions")) {

            // no arguments check
            if (args.length == 0) {
                // send plugin information
                sender.sendMessage(ChatColor.AQUA + " == Medieval Factions " + MedievalFactions.getInstance().getVersion() + " == ");
                sender.sendMessage(ChatColor.AQUA + "Developers: DanTheTechMan, Pasarus, Caibinus");
                sender.sendMessage(ChatColor.AQUA + "Wiki: https://github.com/DansPlugins/Medieval-Factions/wiki");
                return true;
            }

            // argument check
            if (args.length > 0) {

                // default commands ----------------------------------------------------------------------------------

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    HelpCommand command = new HelpCommand();
                    command.sendHelpMessage(sender, args);
                    return true;
                }

                // create command
                if (args[0].equalsIgnoreCase("create") ) {
                    CreateCommand command = new CreateCommand();
                    command.createFaction(sender, args);
                    return true;
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    ListCommand command = new ListCommand();
                    command.listFactions(sender);
                    return true;
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband")) {
                    DisbandCommand command = new DisbandCommand();
                    command.deleteFaction(sender, args);
                    return true;
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    MembersCommand command = new MembersCommand();
                    command.showMembers(sender, args);
                    return true;
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    InfoCommand command = new InfoCommand();
                    command.showInfo(sender, args);
                    return true;
                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    DescCommand command = new DescCommand();
                    command.setDescription(sender, args);
                    return true;
                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    InviteCommand command = new InviteCommand();
                    command.invitePlayer(sender, args);
                    return true;
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    JoinCommand command = new JoinCommand();
                    command.joinFaction(sender, args);
                    return true;
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    KickCommand command = new KickCommand();
                    command.kickPlayer(sender, args);
                    return true;
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    LeaveCommand command = new LeaveCommand();
                    command.leaveFaction(sender);
                    return true;
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    TransferCommand command = new TransferCommand();
                    command.transferOwnership(sender, args);
                    return true;
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar") || args[0].equalsIgnoreCase("dw")) {
                    DeclareWarCommand command = new DeclareWarCommand();
                    command.declareWar(sender, args);
                    return true;
                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace") || args[0].equalsIgnoreCase("mp")) {
                    MakePeaceCommand command = new MakePeaceCommand();
                    command.makePeace(sender, args);
                    return true;
                }

                // TODO: move into command class
                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    if (sender.hasPermission("mf.claim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            // if not at demesne limit
                            if (Utilities.getInstance().isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                                if (ChunkManager.getInstance().getChunksClaimedByFaction(playersFaction.getName(), PersistentData.getInstance().getClaimedChunks()) < playersFaction.getCumulativePowerLevel()) {
                                    ChunkManager.getInstance().addChunkAtPlayerLocation(player);
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

                // TODO: move into command class
                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim")) {
                    if (sender.hasPermission("mf.unclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (Utilities.getInstance().isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                                ChunkManager.getInstance().removeChunkAtPlayerLocation(player);
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

                // TODO: move into command class
                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall") || args[0].equalsIgnoreCase("ua")) {
                    if (sender instanceof Player) {

                        Player player = (Player) sender;

                        if (args.length > 1) {
                            if (player.hasPermission("mf.unclaimall.others") || player.hasPermission("mf.admin")) {

                                String factionName = Utilities.getInstance().createStringFromFirstArgOnwards(args);

                                Faction faction = PersistentData.getInstance().getFaction(factionName);

                                if (faction != null) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    Utilities.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    ChunkManager.getInstance().removeAllClaimedChunks(faction.getName(), PersistentData.getInstance().getClaimedChunks());
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed from " + factionName + "!");

                                    // remove locks associated with this faction
                                    Utilities.getInstance().removeAllLocks(faction.getName(), PersistentData.getInstance().getLockedBlocks());
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

                            for (Faction faction : PersistentData.getInstance().getFactions()) {
                                if (faction.isOwner(player.getUniqueId())) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    Utilities.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    ChunkManager.getInstance().removeAllClaimedChunks(faction.getName(), PersistentData.getInstance().getClaimedChunks());
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed.");

                                    // remove locks associated with this faction
                                    Utilities.getInstance().removeAllLocks(faction.getName(), PersistentData.getInstance().getLockedBlocks());
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

                // TODO: move into command class
                // checkclaim command
                if (args[0].equalsIgnoreCase("checkclaim")|| args[0].equalsIgnoreCase("cc")) {
                    if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            String result = ChunkManager.getInstance().checkOwnershipAtPlayerLocation(player);
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

                // TODO: move into command class
                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim")|| args[0].equalsIgnoreCase("ac")) {
                    if (sender.hasPermission("mf.autoclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            if (Utilities.getInstance().isInFaction(player.getUniqueId(), PersistentData.getInstance().getFactions())) {
                                boolean owner = false;
                                for (Faction faction : PersistentData.getInstance().getFactions()) {
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
                    PromoteCommand command = new PromoteCommand();
                    command.promotePlayer(sender, args);
                    return true;
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    DemoteCommand command = new DemoteCommand();
                    command.demotePlayer(sender, args);
                    return true;
                }

                // power command
                if  (args[0].equalsIgnoreCase("power")) {
                    PowerCommand command = new PowerCommand();
                    command.powerCheck(sender, args);
                    return true;
                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome")|| args[0].equalsIgnoreCase("sh")) {
                    SetHomeCommand command = new SetHomeCommand();
                    command.setHome(sender);
                    return true;
                }

                // home command
                if (args[0].equalsIgnoreCase("home")) {
                    HomeCommand command = new HomeCommand();
                    command.teleportPlayer(sender);
                    return true;
                }

                // TODO: move into command class
                // getVersion() command
                if (args[0].equalsIgnoreCase("getVersion()")) {
                    if (sender.hasPermission("mf.getVersion()") || sender.hasPermission("mf.default")) {
                        sender.sendMessage(ChatColor.AQUA + "Medieval-Factions-" + MedievalFactions.getInstance().getVersion());
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.getVersion()'");
                        return false;
                    }

                }

                // who command
                if (args[0].equalsIgnoreCase("who")) {
                    WhoCommand command = new WhoCommand();
                    command.sendInformation(sender, args);
                    return true;
                }

                // ally command
                if (args[0].equalsIgnoreCase("ally")) {
                    AllyCommand command = new AllyCommand();
                    command.requestAlliance(sender, args);
                    return true;
                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance")|| args[0].equalsIgnoreCase("ba")) {
                    BreakAllianceCommand command = new BreakAllianceCommand();
                    command.breakAlliance(sender, args);
                    return true;
                }

                // rename command
                if (args[0].equalsIgnoreCase("rename")) {
                    RenameCommand command = new RenameCommand();
                    command.renameFaction(sender, args);
                    return true;
                }

                // lock command
                if (args[0].equalsIgnoreCase("lock")) {
                    LockCommand command = new LockCommand();
                    command.lockBlock(sender, args);
                    return true;
                }

                // unlock command
                if (args[0].equalsIgnoreCase("unlock")) {
                    UnlockCommand command = new UnlockCommand();
                    command.unlockBlock(sender, args);
                    return true;
                }

                // grantaccess command
                if (args[0].equalsIgnoreCase("grantaccess")|| args[0].equalsIgnoreCase("ga")) {
                    GrantAccessCommand command = new GrantAccessCommand();
                    command.grantAccess(sender, args);
                    return true;
                }

                // checkaccess command
                if (args[0].equalsIgnoreCase("checkaccess")|| args[0].equalsIgnoreCase("ca")) {
                    CheckAccessCommand command = new CheckAccessCommand();
                    command.checkAccess(sender, args);
                    return true;
                }

                // revokeaccess command
                if (args[0].equalsIgnoreCase("revokeaccess")|| args[0].equalsIgnoreCase("ra")) {
                    RevokeAccessCommand command = new RevokeAccessCommand();
                    command.revokeAccess(sender, args);
                    return true;
                }

                // laws command
                if (args[0].equalsIgnoreCase("laws")) {
                    LawsCommand command = new LawsCommand();
                    command.showLawsToPlayer(sender, args);
                    return true;
                }

                // addlaw command
                if (args[0].equalsIgnoreCase("addlaw")|| args[0].equalsIgnoreCase("al")) {
                    AddLawCommand command = new AddLawCommand();
                    command.addLaw(sender, args);
                    return true;
                }

                // removelaw command
                if (args[0].equalsIgnoreCase("removelaw")|| args[0].equalsIgnoreCase("rl")) {
                    RemoveLawCommand command = new RemoveLawCommand();
                    command.removeLaw(sender, args);
                    return true;
                }

                // editlaw command
                if (args[0].equalsIgnoreCase("editlaw") || args[0].equalsIgnoreCase("el")) {
                    EditLawCommand command = new EditLawCommand();
                    command.editLaw(sender, args);
                    return true;
                }

                // chat command
                if (args[0].equalsIgnoreCase("chat")) {
                    ChatCommand command = new ChatCommand();
                    command.toggleFactionChat(sender);
                    return true;
                }

                // vassalize command
                if (args[0].equalsIgnoreCase("vassalize")) {
                    VassalizeCommand command = new VassalizeCommand();
                    command.sendVassalizationOffer(sender, args);
                    return true;
                }

                // swearfealty command
                if (args[0].equalsIgnoreCase("swearfealty") || args[0].equalsIgnoreCase("sf")) {
                    SwearFealtyCommand command = new SwearFealtyCommand();
                    command.swearFealty(sender, args);
                    return true;
                }

                // declare independence command
                if (args[0].equalsIgnoreCase("declareindependence") || args[0].equalsIgnoreCase("di")) {
                    DeclareIndependenceCommand command = new DeclareIndependenceCommand();
                    command.declareIndependence(sender);
                    return true;
                }

                // grant independence command
                if (args[0].equalsIgnoreCase("grantindependence") || args[0].equalsIgnoreCase("gi")) {
                    GrantIndependenceCommand command = new GrantIndependenceCommand();
                    command.grantIndependence(sender, args);
                    return true;
                }

                // gate management commands
                if (args[0].equalsIgnoreCase("gate") || args[0].equalsIgnoreCase("gt")) {
                	GateCommand command = new GateCommand();
                	command.handleGate(sender, args);
                	return true;
                }
                
                if (args[0].equalsIgnoreCase("duel") || args[0].equalsIgnoreCase("dl")) {
                	DuelCommand command = new DuelCommand();
                	command.handleDuel(sender, args);
                	return true;
                }
                
                // admin commands ----------------------------------------------------------------------------------

                // force command
                if (args[0].equalsIgnoreCase("force")) {
                    ForceCommand command = new ForceCommand();
                    return command.force(sender, args);
                }

                // reset power levels command
                if (args[0].equalsIgnoreCase("resetpowerlevels")|| args[0].equalsIgnoreCase("rpl")) {
                    if (sender.hasPermission("mf.resetpowerlevels") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Power level resetting...");
                        resetPowerRecords();
                        return true;
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.resetpowerlevels'");
                        return false;
                    }
                }

                // bypass command
                if (args[0].equalsIgnoreCase("bypass")) {
                    BypassCommand command = new BypassCommand();
                    command.toggleBypass(sender);
                    return true;
                }

                // config command
                if (args[0].equalsIgnoreCase("config")) {
                    ConfigCommand command = new ConfigCommand();
                    command.handleConfigAccess(sender, args);
                    return true;
                }

            }
            sender.sendMessage(ChatColor.RED + "Medieval Factions doesn't recognize that command!");
        }
        return false;
    }

    private void resetPowerRecords() {
        // reset individual records
        System.out.println("Resetting individual power records.");
        for (PlayerPowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()) {
            record.setPowerLevel(MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
        }
    }

}
