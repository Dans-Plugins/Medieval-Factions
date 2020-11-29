package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.createStringFromFirstArgOnwards;
import static factionsystem.Subsystems.UtilitySubsystem.getPlayersPowerRecord;

public class CreateCommand extends Command {

    public CreateCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public boolean createFaction(CommandSender sender, String[] args) {
        // player check
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // player membership check
            for (Faction faction : main.factions) {
                if (faction.isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Sorry, you're already in a faction. Leave if you want to create a different one.");
                    return false;
                }
            }

            // argument check
            if (args.length > 1) {

                // creating name from arguments 1 to the last one
                String name = createStringFromFirstArgOnwards(args);

                // faction existence check
                boolean factionExists = false;
                for (Faction faction : main.factions) {
                    if (faction.getName().equalsIgnoreCase(name)) {
                        factionExists = true;
                        break;
                    }
                }

                if (!factionExists) {

                    // actual faction creation
                    Faction temp = new Faction(name, player.getUniqueId(), main.getConfig().getInt("initialMaxPowerLevel"), main);
                    main.factions.add(temp);
                    // TODO: Make thread safe
                    main.factions.get(main.factions.size() - 1).addMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());
                    System.out.println("Faction " + name + " created.");
                    player.sendMessage(ChatColor.AQUA + "Faction " + name + " created.");
                    return true;
                }
                else {
                    player.sendMessage(ChatColor.RED + "Sorry! That faction already exists.");
                    return false;
                }
            } else {

                // wrong usage
                sender.sendMessage(ChatColor.RED + "Usage: /mf create [faction-name]");
                return false;
            }
        }
        return false;
    }
}
