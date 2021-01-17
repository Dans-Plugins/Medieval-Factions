package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.Gate;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GateCommand {
	
	public void handleGate(CommandSender sender, String[] args)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			
			if (player.hasPermission("mf.gate"))
			{
				if (args.length > 1)
				{
					if (args[1].equalsIgnoreCase("cancel") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateCancel")))
					{
						if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
						{
							EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CreatingGateCancelled"));
							return;
						}
					}
					if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateCreate")))
					{
						if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
						{
							EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyCreatingGate"));
							return;
						}
						else
						{
							Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
							if (faction != null)
							{
								if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
								{
				        			String gateName = "Unnamed Gate";
				        			if (args.length > 2)
				        			{
				        				gateName = createStringFromArgIndexOnwards(2, args);
				        			}
									startCreatingGate(player, gateName);
									//TODO: Config setting for magic gate tool.
									player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("CreatingGateClickWithHoe"));
									return;
								}
								else
								{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
									return;
								}
							}
						}
					}
					else if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateList")))
					{
						Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
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
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNoGatesDefined"));
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotAMemberOfAnyFaction"));
							return;
						}
					}
					else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateRemove")))
					{
						if (player.getTargetBlock(null, 16) != null)
						{
							if (PersistentData.getInstance().isGateBlock(player.getTargetBlock(null, 16)))
							{
								Gate gate = getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
								Faction faction = getGateFaction(gate, PersistentData.getInstance().getFactions());
								if (faction != null)
								{
									if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
									{
										faction.removeGate(gate);
										player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("RemovedGate"), gate.getName()));
										return;
									}
									else
									{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
										return;
									}
								}
								else
								{
									player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("ErrorCouldNotFindGatesFaction"), gate.getName()));
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TargetBlockNotPartOfGate"));
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NoBlockDetectedToCheckForGate"));
							return;
						}
					}
					else if (args[1].equalsIgnoreCase("name") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateName")))
					{						
						if (player.getTargetBlock(null, 16) != null)
						{
							if (PersistentData.getInstance().isGateBlock(player.getTargetBlock(null, 16)))
							{
								Gate gate = getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
								if (args.length > 2)
								{
									Faction faction = getGateFaction(gate, PersistentData.getInstance().getFactions());
									if (faction != null)
									{
										if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId()))
										{
											String name = createStringFromArgIndexOnwards(2, args);
											gate.setName(name);
											player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertChangedGateName"), gate.getName()));
											return;
										}
										else
										{
											player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToCreateGate"));
											return;
										}
									}
									else
									{
										player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("ErrorCouldNotFindGatesFaction"), gate.getName()));
										return;
									}
								}
								else
								{
									player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertGate"), gate.getName()));
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TargetBlockNotPartOfGate"));
								return;
							}
						}
						else
						{
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NoBlockDetectedToCheckForGate"));
							return;
						}
					}						
				}
				else
				{
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateCreate"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateName"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateList"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateRemove"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateCancel"));
			        return;
				}
			}
            else {
				sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.gate"));
            }

		}
	}

	private void startCreatingGate(Player player, String name)
	{
		if (!EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
		{
			Gate gate = new Gate();
			gate.setName(name);
			EphemeralData.getInstance().getCreatingGatePlayers().put(player.getUniqueId(), gate);
		}
		else
		{
			System.out.println(LocaleManager.getInstance().getText("WarningPlayerAlreadyStartedCreatingGate"));
		}
	}

	private Gate getGate(Block targetBlock, ArrayList<Faction> factions)
	{
		for (Faction faction : factions)
		{
			for (Gate gate : faction.getGates())
			{
				if (gate.hasBlock(targetBlock))
				{
					return gate;
				}
			}
		}
		return null;
	}

	private Faction getGateFaction(Gate gate, ArrayList<Faction> factions)
	{
		for (Faction faction : factions)
		{
			if (faction.getGates().contains(gate))
			{
				return faction;
			}
		}
		return null;
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
