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
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
                    HelpCommand command = new HelpCommand();
                    command.sendHelpMessage(sender, args);
                }
                else {
                    sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
                }
            }

            // argument check
            if (args.length > 0) {

                // default commands ----------------------------------------------------------------------------------

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
                        HelpCommand command = new HelpCommand();
                        command.sendHelpMessage(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
                    }
                }

                // create command
                if (args[0].equalsIgnoreCase("create") ) {
                    if (sender.hasPermission("mf.create")|| sender.hasPermission("mf.default")) {
                        CreateCommand command = new CreateCommand(main);
                        command.createFaction(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.create'");
                    }
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    if (sender.hasPermission("mf.list") || sender.hasPermission("mf.default")) {
                        ListCommand command = new ListCommand(main);
                        command.listFactions(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.list'");
                    }
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband")) {
                    if (sender.hasPermission("mf.disband") || sender.hasPermission("mf.default")) {
                        DisbandCommand command = new DisbandCommand(main);
                        command.deleteFaction(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.disband'");
                    }
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    if (sender.hasPermission("mf.members") || sender.hasPermission("mf.default")) {
                        MembersCommand command = new MembersCommand(main);
                        command.showMembers(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.members'");
                    }
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    if (sender.hasPermission("mf.info") || sender.hasPermission("mf.default")) {
                        InfoCommand command = new InfoCommand(main);
                        command.showInfo(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.info'");
                    }

                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    if (sender.hasPermission("mf.desc") || sender.hasPermission("mf.default")) {
                        DescCommand command = new DescCommand(main);
                        command.setDescription(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.desc'");
                    }

                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    if (sender.hasPermission("mf.invite") || sender.hasPermission("mf.default")) {
                        InviteCommand command = new InviteCommand(main);
                        command.invitePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.invite'");
                    }
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    if (sender.hasPermission("mf.join") || sender.hasPermission("mf.default")) {
                        JoinCommand command = new JoinCommand(main);
                        command.joinFaction(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.join'");
                    }
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    if (sender.hasPermission("mf.kick") || sender.hasPermission("mf.default")) {
                        KickCommand command = new KickCommand(main);
                        command.kickPlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.kick'");
                    }
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    if (sender.hasPermission("mf.leave") || sender.hasPermission("mf.default")) {
                        LeaveCommand command = new LeaveCommand(main);
                        command.leaveFaction(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.leave'");
                    }
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    if (sender.hasPermission("mf.transfer") || sender.hasPermission("mf.default")) {
                        TransferCommand command = new TransferCommand(main);
                        command.transferOwnership(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.transfer'");
                    }
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar") || args[0].equalsIgnoreCase("dw")) {
                    if (sender.hasPermission("mf.declarewar") || sender.hasPermission("mf.default")) {
                        DeclareWarCommand command = new DeclareWarCommand(main);
                        command.declareWar(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.declarewar'");
                    }

                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace") || args[0].equalsIgnoreCase("mp")) {
                    if (sender.hasPermission("mf.makepeace") || sender.hasPermission("mf.default")) {
                        MakePeaceCommand command = new MakePeaceCommand(main);
                        command.makePeace(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.makepeace'");
                    }
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    if (sender.hasPermission("mf.claim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            // if not at demesne limit
                            if (isInFaction(player.getName(), main.factions)) {
                                Faction playersFaction = getPlayersFaction(player.getName(), main.factions);
                                if (getChunksClaimedByFaction(playersFaction.getName(), main.claimedChunks) < playersFaction.getCumulativePowerLevel()) {
                                    main.utilities.addChunkAtPlayerLocation(player);
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You must be in a faction to use this command.");
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.claim'");
                    }
                }

                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim")) {
                    if (sender.hasPermission("mf.unclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (isInFaction(player.getName(), main.factions)) {
                                main.utilities.removeChunkAtPlayerLocation(player);
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaim'");
                    }
                }

                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall")|| args[0].equalsIgnoreCase("ua")) {
                    if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            for (Faction faction : main.factions) {
                                if (faction.isOwner(player.getName())) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    removeAllClaimedChunks(faction.getName(), main.claimedChunks);
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed.");

                                    // remove locks associated with this faction
                                    removeAllLocks(faction.getName(), main.lockedBlocks);
                                }
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
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
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "This land is claimed by " + result + ".");
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
                    }
                }

                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim")|| args[0].equalsIgnoreCase("ac")) {
                    if (sender.hasPermission("mf.autoclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            if (isInFaction(player.getName(), main.factions)) {
                                boolean owner = false;
                                for (Faction faction : main.factions) {
                                    if (faction.isOwner(player.getName())) {
                                        owner = true;
                                        faction.toggleAutoClaim();
                                        player.sendMessage(ChatColor.AQUA + "Autoclaim toggled.");
                                    }

                                }
                                if (!owner) {
                                    player.sendMessage(ChatColor.RED + "You must be the owner to use this command.");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.autoclaim'");
                    }
                }

                // promote command
                if (args[0].equalsIgnoreCase("promote")) {
                    if (sender.hasPermission("mf.promote") || sender.hasPermission("mf.default")) {
                        PromoteCommand command = new PromoteCommand(main);
                        command.promotePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.promote'");
                    }
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    if (sender.hasPermission("mf.demote")) {
                        DemoteCommand command = new DemoteCommand(main);
                        command.demotePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.demote'");
                    }
                }

                // power command
                if  (args[0].equalsIgnoreCase("power")) {
                    if (sender.hasPermission("mf.power") || sender.hasPermission("mf.default")) {
                        PowerCommand command = new PowerCommand(main);
                        command.powerCheck(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.power'");
                    }

                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome")|| args[0].equalsIgnoreCase("sh")) {
                    if (sender.hasPermission("mf.sethome") || sender.hasPermission("mf.default")) {
                        SetHomeCommand command = new SetHomeCommand(main);
                        command.setHome(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.sethome'");
                    }
                }

                // home command
                if (args[0].equalsIgnoreCase("home")) {
                    if (sender.hasPermission("mf.home") || sender.hasPermission("mf.default")) {
                        HomeCommand command = new HomeCommand(main);
                        command.teleportPlayer(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.home'");
                    }
                }

                // version command
                if (args[0].equalsIgnoreCase("version")) {
                    if (sender.hasPermission("mf.version") || sender.hasPermission("mf.default")) {
                        sender.sendMessage(ChatColor.AQUA + "Medieval-Factions-" + main.version);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.version'");
                    }

                }

                // who command
                if (args[0].equalsIgnoreCase("who")) {
                    if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                        WhoCommand command = new WhoCommand(main);
                        command.sendInformation(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.who'");
                    }

                }

                // ally command
                if (args[0].equalsIgnoreCase("ally")) {
                    if (sender.hasPermission("mf.ally") || sender.hasPermission("mf.default")) {
                        AllyCommand command = new AllyCommand(main);
                        command.requestAlliance(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.ally'");
                    }

                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance")|| args[0].equalsIgnoreCase("ba")) {
                    if (sender.hasPermission("mf.breakalliance") || sender.hasPermission("mf.default")) {
                        BreakAllianceCommand command = new BreakAllianceCommand(main);
                        command.breakAlliance(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.breakalliance'");
                    }
                }

                // TODO: shift responsibility of perm checking from Main to the Command class of each command, like below

                // rename command
                if (args[0].equalsIgnoreCase("rename")) {
                    RenameCommand command = new RenameCommand(main);
                    command.renameFaction(sender, args);
                }

                // lock command
                if (args[0].equalsIgnoreCase("lock")) {
                    LockCommand command = new LockCommand(main);
                    command.lockBlock(sender, args);
                }

                // unlock command
                if (args[0].equalsIgnoreCase("unlock")) {
                    UnlockCommand command = new UnlockCommand(main);
                    command.unlockBlock(sender, args);
                }

                // grantaccess command
                if (args[0].equalsIgnoreCase("grantaccess")|| args[0].equalsIgnoreCase("ga")) {
                    GrantAccessCommand command = new GrantAccessCommand(main);
                    command.grantAccess(sender, args);
                }

                // checkaccess command
                if (args[0].equalsIgnoreCase("checkaccess")|| args[0].equalsIgnoreCase("ca")) {
                    CheckAccessCommand command = new CheckAccessCommand(main);
                    command.checkAccess(sender, args);
                }

                // revokeaccess command
                if (args[0].equalsIgnoreCase("revokeaccess")|| args[0].equalsIgnoreCase("ra")) {
                    RevokeAccessCommand command = new RevokeAccessCommand(main);
                    command.revokeAccess(sender, args);
                }

                // laws command
                if (args[0].equalsIgnoreCase("laws")) {
                    LawsCommand command = new LawsCommand(main);
                    command.showLawsToPlayer(sender);
                }

                // addlaw command
                if (args[0].equalsIgnoreCase("addlaw")|| args[0].equalsIgnoreCase("al")) {
                    AddLawCommand command = new AddLawCommand(main);
                    command.addLaw(sender, args);
                }

                // removelaw command
                if (args[0].equalsIgnoreCase("removelaw")|| args[0].equalsIgnoreCase("rl")) {
                    RemoveLawCommand command = new RemoveLawCommand(main);
                    command.removeLaw(sender, args);
                }

                // admin commands ----------------------------------------------------------------------------------

                // forcesave command
                if (args[0].equalsIgnoreCase("forcesave")|| args[0].equalsIgnoreCase("fs")) {
                    if (sender.hasPermission("mf.forcesave") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is saving...");
                        main.storage.save();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forcesave'");
                    }
                }

                // forceload command
                if (args[0].equalsIgnoreCase("forceload")|| args[0].equalsIgnoreCase("fl")) {
                    if (sender.hasPermission("mf.forceload") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is loading...");
                        main.storage.load();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forceload'");
                    }
                }

                // reset power levels command
                if (args[0].equalsIgnoreCase("resetpowerlevels")|| args[0].equalsIgnoreCase("rpl")) {
                    if (sender.hasPermission("mf.resetpowerlevels") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Power level resetting...");
                        main.utilities.resetPowerRecords();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.resetpowerlevels'");
                    }
                }

            }
        }
        return true;
    }

}
