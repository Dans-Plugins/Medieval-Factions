package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.createStringFromFirstArgOnwards;
import static factionsystem.Subsystems.UtilitySubsystem.sendAllPlayersInFactionMessage;

public class BreakAllianceCommand extends Command {

    public BreakAllianceCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void breakAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.breakalliance") || sender.hasPermission("mf.default")) {
                boolean owner = false;
                for (Faction faction : main.factions) {
                    // if player is the owner or officer
                    if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                        owner = true;
                        // if there's more than one argument
                        if (args.length > 1) {

                            // get name of faction
                            String factionName = createStringFromFirstArgOnwards(args);

                            // check if faction exists
                            for (int i = 0; i < main.factions.size(); i++) {
                                if (main.factions.get(i).getName().equalsIgnoreCase(factionName)) {

                                    if (!(faction.getName().equalsIgnoreCase(factionName))) {

                                        // check that enemy is not already on list
                                        if ((faction.isAlly(factionName))) {
                                            // remove alliance
                                            faction.removeAlly(factionName);
                                            player.sendMessage(ChatColor.GREEN + "Alliance has been broken with " + factionName + "!");

                                            // add declarer's faction to new enemy's enemyList
                                            main.factions.get(i).removeAlly(faction.getName());
                                            for (int j = 0; j < main.factions.size(); j++) {
                                                if (main.factions.get(j).getName().equalsIgnoreCase(factionName)) {
                                                    sendAllPlayersInFactionMessage(main.factions.get(j), ChatColor.RED + faction.getName() + " has broken their alliance your faction!");
                                                }
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "Your faction is not allied with " + factionName);
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "You can't declare break an alliance with your own faction.");
                                    }
                                }
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf breakalliance (faction-name)");
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + "You have to own a faction or be an officer of a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.breakalliance'");
            }
        }
    }
}
