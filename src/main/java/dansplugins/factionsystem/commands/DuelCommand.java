package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.domainobjects.Duel;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand {
	
	public void handleDuel(CommandSender sender, String[] args) {
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			
			if (player.hasPermission("mf.duel") || player.hasPermission("mf.default"))
			{
				if (args.length > 1)
				{
					if (args[1].equalsIgnoreCase("challenge"))
					{
						if (args.length > 2)
						{
							if (args[2].equalsIgnoreCase(player.getName()))
							{
								player.sendMessage(ChatColor.RED + "You cannot duel yourself!");
								return;
							}
							if (isDuelling(player))
							{
								player.sendMessage(ChatColor.RED + "You are already duelling someone!");
								return;
							}
							Player target = Bukkit.getServer().getPlayer(args[2]);
							if (target != null)
							{
								if (!isDuelling(target))
								{
									int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
									if (args.length == 4)
									{
										timeLimit = Integer.parseInt(args[3]);
									}
									inviteDuel(player, target, timeLimit);
									player.sendMessage(ChatColor.AQUA + "You have challenged " + target.getName() + " to a duel!");
									return;
								}
								else
								{
									player.sendMessage(ChatColor.RED + target.getName() + " is already duelling someone!");
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + "Could not find any player named '" + args[2] + "'.");
								return;
							}
						}
					}
					else if (args[1].equalsIgnoreCase("accept"))
					{
						if (isDuelling(player))
						{
							player.sendMessage(ChatColor.RED + "You are already duelling someone!");
							return;
						}
						// If a name is specified to accept the challenge from, look for that specific name.
						if (args.length > 2)
						{
		                	Player challenger = Bukkit.getServer().getPlayer(args[2]);
		                	Duel duel = Utilities.getDuel(challenger, player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + "You are already duelling " + args[2] + "!");
									return;
		                		}
		                		if (duel.isChallenged(player))
		                		{
		                			duel.acceptDuel();
		                		}
		                		else
		                		{
									player.sendMessage(ChatColor.RED + "You have not been challenged to a duel by '" + args[2] + "'.");
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.RED + "You have not been challenged to a duel by '" + args[2] + "'.");
								return;
		                	}
						}
						else
						{
		                	Duel duel = Utilities.getDuel(player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + "You are already duelling!");
									return;
		                		}
		                		if (duel.isChallenged(player))
		                		{
		                			duel.acceptDuel();
		                		}
		                		else
		                		{
									player.sendMessage(ChatColor.RED + "You have not been challenged to a duel by anyone.");
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.RED + "You have not been challenged to a duel by anyone.");
								return;
		                	}
						}
					}
					else if (args[1].equalsIgnoreCase("cancel"))
					{
		                if (isDuelling(player))
		                {
		                	Duel duel = Utilities.getDuel(player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + "Cannot cancel an active duel.");
									return;
		                		}
		                		else
		                		{
		                			EphemeralData.getInstance().getDuelingPlayers().remove(duel);
									player.sendMessage(ChatColor.AQUA + "Duel challenge cancelled.");
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.AQUA + "You have no pending challenges to cancel.");
								return;
		                	}
		                }
	                	else
	                	{
							player.sendMessage(ChatColor.AQUA + "You have no pending challenges to cancel.");
							return;
	                	}

					}
					else
					{
				        sender.sendMessage(ChatColor.RED + "Sub-commands:");
				        sender.sendMessage(ChatColor.RED + "/mf duel challenge (player)");
				        sender.sendMessage(ChatColor.RED + "/mf duel accept (<optional>player)");
				        sender.sendMessage(ChatColor.RED + "/mf duel cancel");
					}
				}
				else
				{
			        sender.sendMessage(ChatColor.RED + "Sub-commands:");
			        sender.sendMessage(ChatColor.RED + "/mf duel challenge (player) (<optional>time limit in seconds)");
			        sender.sendMessage(ChatColor.RED + "/mf duel accept (<optional>player)");
			        sender.sendMessage(ChatColor.RED + "/mf duel cancel");
				}
			}
		}
	}

	private boolean isDuelling(Player player)
	{
		for (Duel duel : EphemeralData.getInstance().getDuelingPlayers())
		{
			if (duel.hasPlayer(player) && duel.getStatus().equals(Duel.DuelState.DUELLING))
			{
				return true;
			}
		}
		return false;
	}

	private void inviteDuel(Player player, Player target, int limit)
	{
		target.sendMessage(ChatColor.AQUA + player.getName() + " has challenged you to a duel! Type /mf duel accept to begin.");
		EphemeralData.getInstance().getDuelingPlayers().add(new Duel(player, target, limit));
	}
}
