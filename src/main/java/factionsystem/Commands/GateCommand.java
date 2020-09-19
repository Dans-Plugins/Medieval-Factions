package factionsystem.Commands;

import static factionsystem.Subsystems.UtilitySubsystem.createStringFromFirstArgOnwards;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import factionsystem.Objects.Gate;
import factionsystem.Subsystems.UtilitySubsystem;

public class GateCommand {

	Main main = null;
	
	public GateCommand(Main plugin)
	{
		main = plugin;
	}
	
	public void handleGate(CommandSender sender, String[] args)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			
			if (player.hasPermission("mf.gate") || player.hasPermission("mf.default"))
			{
				if (args.length > 1)
				{
					if (args[1].equalsIgnoreCase("cancel"))
					{
						if (main.creatingGatePlayers.containsKey(player.getUniqueId()))
						{
							main.creatingGatePlayers.remove(player.getUniqueId());
							player.sendMessage(ChatColor.RED + "Creating gate cancelled!");
							return;
						}
					}
					if (args[1].equalsIgnoreCase("list"))
					{
						Faction faction = UtilitySubsystem.getPlayersFaction(player.getUniqueId(), main.factions);
						if (faction != null)
						{
							player.sendMessage(ChatColor.AQUA + "Faction Gates");
							for (Gate gate : faction.getGates())
							{
								player.sendMessage(ChatColor.AQUA + String.format("%s: %s", gate.getName(), gate.coordsToString()));
							}
						}
					}
					if (args[1].equalsIgnoreCase("name"))
					{
						if (player.getLineOfSight((Set<Material>) null, 16).size() > 0)
						{
							if (UtilitySubsystem.isGateBlock(player.getLineOfSight((Set<Material>) null, 16).get(0), main.factions))
							{
								Gate gate = UtilitySubsystem.getGate(player.getLineOfSight((Set<Material>) null, 16).get(0), main.factions);
								if (args.length > 2)
								{
									String name = UtilitySubsystem.createStringFromArgIndexOnwards(2, args);
									gate.setName(name);
								}
								else
								{
									player.sendMessage(ChatColor.AQUA + String.format("%s Gate.", gate.getName()));
									return;
								}
							}
						}
					}						
				}
			}
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.gate'");
            }

		}
	}
	
}
