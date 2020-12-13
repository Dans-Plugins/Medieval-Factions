package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.Gate;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GateCommand {
	
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
						if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
						{
							EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
							player.sendMessage(ChatColor.RED + "Creating gate cancelled!");
							return;
						}
					}
					if (args[1].equalsIgnoreCase("create"))
					{
						if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
						{
							EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
							player.sendMessage(ChatColor.RED + "You are already creating a gate!");
							return;
						}
						else
						{
							Faction faction = Utilities.getInstance().getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
							if (faction != null)
							{
								if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
								{
				        			String gateName = "Unnamed Gate";
				        			if (args.length > 2)
				        			{
				        				gateName = createStringFromArgIndexOnwards(2, args);
				        			}
									Utilities.getInstance().startCreatingGate(player, gateName);
									//TODO: Config setting for magic gate tool.
									player.sendMessage(ChatColor.AQUA + "Creating gate '" + gateName + "'.\nClick on a block with a Golden Hoe to select the first point.");
									return;
								}
								else
								{
									player.sendMessage(ChatColor.RED + "You must be a faction officer or owner to use this command.");
									return;
								}
							}
						}
					}
					else if (args[1].equalsIgnoreCase("list"))
					{
						Faction faction = Utilities.getInstance().getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
						if (faction != null)
						{
							if (faction.getGates().size() > 0)
							{
								player.sendMessage(ChatColor.AQUA + "Faction Gates");
								for (Gate gate : faction.getGates())
								{
									player.sendMessage(ChatColor.AQUA + String.format("%s: %s", gate.getName(), gate.coordsToString()));
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + "Your faction has no gates defined.");
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + String.format("You are not a member of any faction."));
							return;
						}
					}
					else if (args[1].equalsIgnoreCase("remove"))
					{
						if (player.getTargetBlock(null, 16) != null)
						{
							if (Utilities.getInstance().isGateBlock(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions()))
							{
								Gate gate = Utilities.getInstance().getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
								Faction faction = Utilities.getInstance().getGateFaction(gate, PersistentData.getInstance().getFactions());
								if (faction != null)
								{
									if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
									{
										faction.removeGate(gate);
										player.sendMessage(ChatColor.AQUA + String.format("Removed gate '%s'.", gate.getName()));
										return;
									}
									else
									{
										player.sendMessage(ChatColor.RED + "You must be a faction officer or owner to use this command.");
										return;
									}
								}
								else
								{
									player.sendMessage(ChatColor.RED + String.format("Error: Could not find gate faction.", gate.getName()));
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + "Target block is not part of a gate.");
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + "No block detected to check for gate.");
							return;
						}
					}
					else if (args[1].equalsIgnoreCase("name"))
					{						
						if (player.getTargetBlock(null, 16) != null)
						{
							if (Utilities.getInstance().isGateBlock(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions()))
							{
								Gate gate = Utilities.getInstance().getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
								if (args.length > 2)
								{
									Faction faction = Utilities.getInstance().getGateFaction(gate, PersistentData.getInstance().getFactions());
									if (faction != null)
									{
										if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
										{
											String name = createStringFromArgIndexOnwards(2, args);
											gate.setName(name);
											player.sendMessage(ChatColor.AQUA + String.format("Changed gate name to '%s'.", gate.getName()));
											return;
										}
										else
										{
											player.sendMessage(ChatColor.RED + "You must be a faction officer or owner to use this command.");
											return;
										}
									}
									else
									{
										player.sendMessage(ChatColor.RED + "Error: Could not find gate's faction.");
										return;
									}
								}
								else
								{
									player.sendMessage(ChatColor.AQUA + String.format("That is the '%s' gate.", gate.getName()));
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + "Target block is not part of a gate.");
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + "No block detected to check for gate.");
							return;
						}
					}						
				}
				else
				{
			        sender.sendMessage(ChatColor.RED + "Sub-commands:");
			        sender.sendMessage(ChatColor.AQUA + "/mf gate create (<optional>name)");
			        sender.sendMessage(ChatColor.RED + "/mf gate name (<optional>name)");
			        sender.sendMessage(ChatColor.RED + "/mf gate list");
			        sender.sendMessage(ChatColor.RED + "/mf gate remove");
			        sender.sendMessage(ChatColor.RED + "/mf gate cancel");
			        return;
				}
			}
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.gate'");
            }

		}
	}

	private String createStringFromArgIndexOnwards(int index, String[] args) {
		StringBuilder name = new StringBuilder();
		for (int i = index; i < args.length; i++) {
			name.append(args[i]);
			if (!(i == args.length - 1)) {
				name.append(" ");
			}
		}
		return name.toString();
	}
	
}
